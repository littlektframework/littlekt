package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.graph.node.ui.Control
import com.littlekt.graph.node.ui.column
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.random

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class PBRExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        this.addCloseOnEsc()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 1.5f, 0f)
        val environment = PBREnvironment(device)
        environment.setDirectionalLight(
            DirectionalLight(color = Color(0.2f, 0.2f, 0.2f), intensity = 0.1f)
        )
        environment.setAmbientLight(AmbientLight(color = Color(0.002f, 0.002f, 0.002f)))
        environment.addPointLight(PointLight(Vec3f(0f, 2.5f, 0f), color = Color.GREEN, range = 4f))
        environment.addPointLight(
            PointLight(Vec3f(8.95f, 2f, 3.15f), range = 4f, color = Color.RED)
        )
        environment.addPointLight(
            PointLight(Vec3f(-9.45f, 2f, -3.59f), range = 4f, color = Color.BLUE)
        )
        environment.addPointLight(
            PointLight(Vec3f(-9.45f, 2f, 3.15f), range = 4f, color = Color.GREEN)
        )
        environment.addPointLight(
            PointLight(Vec3f(8.95f, 2f, -3.59f), range = 4f, color = Color.YELLOW)
        )

        repeat(1024) {
            environment.addPointLight(
                PointLight(
                    Vec3f(
                        (-9.45f..8.95f).random(),
                        (0.5f..7.2f).random(),
                        (-3.59f..3.15f).random(),
                    ),
                    range = 2f,
                    color = Color((0f..1f).random(), (0f..1f).random(), (0f..1f).random(), 1f),
                )
            )
        }

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val graph =
            sceneGraph(this) {
                    column {
                        anchor(Control.AnchorLayout.TOP_LEFT)
                        marginLeft = 10f
                        var fps = -1
                        var lowest = -1
                        var ticksToWaitBeforeTracking = 100

                        onUpdate {
                            if (ticksToWaitBeforeTracking > 0) {
                                ticksToWaitBeforeTracking--
                                return@onUpdate
                            }
                            fps = stats.fps.toInt()
                            if (lowest < 0 && fps > 0) {
                                lowest = fps
                            }

                            if (fps < lowest) {
                                lowest = fps
                            }
                            if (input.isKeyJustPressed(Key.R)) {
                                lowest = -1
                            }
                        }
                        label { onUpdate { text = "FPS: $fps" } }
                        label { onUpdate { text = "Lowest: $lowest" } }
                    }
                }
                .also { it.initialize() }

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
                GltfModel(
                    resourcesVfs["models/sponza.glb"],
                    GltfLoaderPbrConfig(),
                    environment,
                    1f,
                    Vec3f.ZERO,
                )
            )

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                this.depthFormat = depthFormat
            }

        loadGltfModels(models, modelBatch)

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

            graph.resize(width, height)
        }

        addFlyController(camera)

        onUpdate {
            if (input.isKeyJustPressed(Key.T)) {
                println(camera.position)
            }
        }

        onUpdate { dt -> models.forEach { model -> model.scene?.update(dt) } }

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

            modelBatch.renderGltfModels(models, camera)
            modelBatch.flush(renderPassEncoder, camera, dt)
            renderPassEncoder.end()
            renderPassEncoder.release()

            graph.update(dt)
            graph.render(
                commandEncoder,
                RenderPassDescriptor(
                    listOf(
                        RenderPassColorAttachmentDescriptor(
                            view = frame,
                            loadOp = LoadOp.LOAD,
                            storeOp = StoreOp.STORE,
                        )
                    ),
                    label = "ui render pass",
                ),
            )

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
