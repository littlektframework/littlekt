package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.ByteBuffer
import com.littlekt.file.gltf.GltfModelUnlitConfig
import com.littlekt.file.gltf.toModel
import com.littlekt.file.vfs.readGltf
import com.littlekt.graphics.*
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.g3d.util.CameraSimpleBuffers
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.geom.degrees
import com.littlekt.util.milliseconds

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SimpleGltfExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnShiftEsc()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 25f, 150f)
        val cameraBuffers = CameraSimpleBuffers(device)

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        val queue = device.queue

        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8
        var depthTexture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(graphics.width, graphics.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    depthFormat,
                    TextureUsage.RENDER_ATTACHMENT,
                )
            )
        var depthFrame = depthTexture.createView()
        val whiteTexture =
            PixmapTexture(
                device,
                preferredFormat,
                Pixmap(1, 1, ByteBuffer(byteArrayOf(1, 1, 1, 1))),
            )
        val models =
            listOf(
                resourcesVfs["models/Duck.glb"]
                    .readGltf()
                    .toModel(config = GltfModelUnlitConfig())
                    .apply {
                        scale(20f)
                        translate(-30f, 0f, 0f)
                    },
                resourcesVfs["models/Fox.glb"]
                    .readGltf()
                    .toModel(config = GltfModelUnlitConfig())
                    .apply { translate(30f, 0f, 0f) },
                resourcesVfs["models/flighthelmet/FlightHelmet.gltf"]
                    .readGltf()
                    .toModel(config = GltfModelUnlitConfig())
                    .apply {
                        scale(200f)
                        translate(90f, 0f, 0f)
                    },
            )

        val grid =
            MeshNode(
                    fullIndexedMesh().generate {
                        grid {
                            sizeX = 1000f
                            sizeY = 1000f
                        }
                    },
                    UnlitMaterial(whiteTexture),
                )
                .apply { translate(0f, -30f, 0f) }

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                colorFormat = preferredFormat
                this.depthFormat = depthFormat
            }

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        onResize { width, height ->
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
            depthFrame.release()
            depthTexture.release()
            depthTexture =
                device.createTexture(
                    TextureDescriptor(
                        Extent3D(width, height, 1),
                        1,
                        1,
                        TextureDimension.D2,
                        depthFormat,
                        TextureUsage.RENDER_ATTACHMENT,
                    )
                )
            depthFrame = depthTexture.createView()
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.5f)
        onUpdate { dt ->
            models.forEach { model ->
                model.rotate(y = 0.1.degrees * dt.milliseconds)
                model.update(dt)
            }
        }
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                TextureStatus.TIMEOUT,
                TextureStatus.OUTDATED,
                TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }
                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                    return@onUpdate
                }
            }
            camera.update()

            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor =
                                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                                        else Color.DARK_GRAY,
                                )
                            ),
                            depthStencilAttachment =
                                RenderPassDepthStencilAttachmentDescriptor(
                                    view = depthFrame,
                                    depthClearValue = 1f,
                                    depthLoadOp = LoadOp.CLEAR,
                                    depthStoreOp = StoreOp.STORE,
                                    stencilClearValue = 0,
                                    stencilLoadOp = LoadOp.CLEAR,
                                    stencilStoreOp = StoreOp.STORE,
                                ),
                        )
                )

            models.forEach { model -> modelBatch.render(model, cameraBuffers) }
            modelBatch.render(grid, cameraBuffers)
            modelBatch.flush(renderPassEncoder, camera, cameraBuffers)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease { depthTexture.release() }
    }
}
