package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfModelUnlitConfig
import com.littlekt.file.gltf.toModel
import com.littlekt.file.vfs.readGltf
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.ModelBatch
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
        addCloseOnEsc()
        val device = graphics.device
        val camera = PerspectiveCamera()
        camera.translate(0f, 25f, 150f)

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
        }

        addWASDMovement(camera, 0.5f)
        addZoom(camera, 0.01f)
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

            models.forEach { model -> modelBatch.render(model) }
            modelBatch.flush(renderPassEncoder, camera.viewProjection)
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
