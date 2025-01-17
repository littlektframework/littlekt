package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.async.KtScope
import com.littlekt.file.gltf.GltfModelUnlitConfig
import com.littlekt.file.gltf.toModel
import com.littlekt.file.vfs.readGltf
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Scene
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.geom.degrees
import com.littlekt.util.seconds
import kotlinx.coroutines.launch

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class ModelInstancingExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnShiftEsc()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(900f, 900f, 1500f)
        val environment = UnlitEnvironment(device)

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
        val duckModel =
            resourcesVfs["models/Duck.glb"].readGltf().toModel(config = GltfModelUnlitConfig())
        val duckModelInstances = mutableListOf<Scene>()

        KtScope.launch {
            repeat(30) { y ->
                repeat(30) { x ->
                    duckModelInstances +=
                        duckModel.createInstance().apply {
                            rotate(
                                (0..360).random().degrees,
                                (0..360).random().degrees,
                                (0..360).random().degrees,
                            )
                            translate(x * 200f, y * 200f, 0f)
                        }
                }
            }
        }

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
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
            duckModel.rotate(y = 0.1.degrees * dt.seconds)
            duckModelInstances.forEach { instance ->
                instance.rotate(
                    x = 10f.degrees * dt.seconds,
                    y = 10f.degrees * dt.seconds,
                    z = 10f.degrees * dt.seconds,
                )
            }
        }
        onUpdate { dt ->
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

            modelBatch.render(duckModel, environment)
            modelBatch.flush(renderPassEncoder, camera, dt)
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