package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.examples.debug.DepthMaterial
import com.littlekt.examples.debug.DepthMaterialPipelineProvider
import com.littlekt.examples.debug.DepthSliceMaterial
import com.littlekt.examples.debug.DepthSliceMaterialPipelineProvider
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.graph.node.ui.Control
import com.littlekt.graph.node.ui.column
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.TonemapMode
import com.littlekt.graphics.Tonemapper
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.shadow.DirectionalShadowMap
import com.littlekt.graphics.g3d.shadow.ShadowMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.*
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.random
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.toString

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class PBRExample(context: Context) : ContextListener(context) {

    private enum class RenderView {
        NORMAL,
        DEPTH,
        DEPTH_SLICE,
    }

    override suspend fun Context.start() {

        addStatsHandler()
        this.addCloseOnEsc()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 1.5f, 0f)
        camera.rotate((-10).degrees, Vec3f.X_AXIS)
        camera.rotate((-90).degrees, Vec3f.Y_AXIS)
        val depthSliceMaterial = DepthSliceMaterial(device)
        val depthMaterial = DepthMaterial(device)
        val shadowMap = DirectionalShadowMap(device, cascadeCount = 4).apply { shadowDistance = 90f }
        val sunDirection = MutableVec3f(0.25f, 1f, 0.15f).norm()
        val pbrEnvironment = PBREnvironment(device, shadowMap = shadowMap)
        val shadowBatch =
            ModelBatch(device).apply {
                defaultPipelineProvider = ShadowMaterialPipelineProvider()
                this.depthFormat = shadowMap.format
            }
        val shadowCasters = mutableListOf<Node3D>()
        val simpleEnvironment = UnlitEnvironment(device)
        pbrEnvironment.setDirectionalLight(
            DirectionalLight(
                color = Color(1f, 0.95f, 0.85f),
                direction = sunDirection,
                intensity = 4f,
            )
        )
        pbrEnvironment.setAmbientLight(AmbientLight(color = Color(0.002f, 0.002f, 0.002f)))
        pbrEnvironment.addPointLight(
            PointLight(Vec3f(0f, 2.5f, 0f), color = Color.GREEN, range = 4f)
        )
        pbrEnvironment.addPointLight(
            PointLight(Vec3f(8.95f, 2f, 3.15f), range = 4f, color = Color.RED)
        )
        pbrEnvironment.addPointLight(
            PointLight(Vec3f(-9.45f, 2f, -3.59f), range = 4f, color = Color.BLUE)
        )
        pbrEnvironment.addPointLight(
            PointLight(Vec3f(-9.45f, 2f, 3.15f), range = 4f, color = Color.GREEN)
        )
        pbrEnvironment.addPointLight(
            PointLight(Vec3f(8.95f, 2f, -3.59f), range = 4f, color = Color.YELLOW)
        )

        repeat(1024) {
            pbrEnvironment.addPointLight(
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
        val tonemapper = Tonemapper(device, preferredFormat)
        val hdrFormat = TextureFormat.RGBA16_FLOAT
        val clearColor = Color.DARK_GRAY.toLinear()
        var hdrTexture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(graphics.width, graphics.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    hdrFormat,
                    TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                )
            )
        var hdrFrame = hdrTexture.createView()
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
                        label { onUpdate { text = "Tonemap (M): ${tonemapper.mode}" } }
                        label {
                            onUpdate {
                                text = "Exposure (PgUp/PgDn): ${tonemapper.exposure.toString(2)}"
                            }
                        }
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
                    pbrEnvironment,
                    1f,
                    Vec3f.ZERO,
                )
            )

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
                addPipelineProvider(DepthSliceMaterialPipelineProvider())
                addPipelineProvider(DepthMaterialPipelineProvider())
                colorFormat = hdrFormat
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
            hdrFrame.release()
            hdrTexture.release()
            hdrTexture =
                device.createTexture(
                    TextureDescriptor(
                        Extent3D(width, height, 1),
                        1,
                        1,
                        TextureDimension.D2,
                        hdrFormat,
                        TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                    )
                )
            hdrFrame = hdrTexture.createView()
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()

            graph.resize(width, height)
        }
        var renderView = RenderView.NORMAL

        addFlyController(camera)

        onUpdate {
            if (input.isKeyJustPressed(Key.T)) {
                println(camera.position)
            }
            if (input.isKeyJustPressed(Key.NUM1) && renderView != RenderView.NORMAL) {
                renderView = RenderView.NORMAL
            }

            if (input.isKeyJustPressed(Key.NUM2) && renderView != RenderView.DEPTH_SLICE) {
                renderView = RenderView.DEPTH_SLICE
            }

            if (input.isKeyJustPressed(Key.NUM3) && renderView != RenderView.DEPTH) {
                renderView = RenderView.DEPTH
            }

            if (input.isKeyJustPressed(Key.M)) {
                val modes = TonemapMode.entries
                tonemapper.mode = modes[(tonemapper.mode.ordinal + 1) % modes.size]
            }
            if (input.isKeyJustPressed(Key.PAGE_UP)) {
                tonemapper.exposure *= 1.25f
            }
            if (input.isKeyJustPressed(Key.PAGE_DOWN)) {
                tonemapper.exposure /= 1.25f
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

            shadowCasters.clear()
            models.fastForEach { model -> model.scene?.let { shadowCasters += it } }
            shadowMap.update(sunDirection, camera)
            shadowMap.render(commandEncoder, shadowBatch, shadowCasters, dt)
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = hdrFrame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor = clearColor,
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

            when (renderView) {
                RenderView.NORMAL -> modelBatch.renderGltfModels(models, camera)
                RenderView.DEPTH ->
                    models.fastForEach { model ->
                        model.scene?.let {
                            modelBatch.render(it, camera, depthMaterial, simpleEnvironment)
                        }
                    }
                RenderView.DEPTH_SLICE ->
                    models.fastForEach { model ->
                        model.scene?.let {
                            modelBatch.render(it, camera, depthSliceMaterial, simpleEnvironment)
                        }
                    }
            }

            modelBatch.flush(renderPassEncoder, camera, dt)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val tonemapPassEncoder =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = frame,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                            )
                        ),
                        label = "tonemap render pass",
                    )
                )
            tonemapper.render(tonemapPassEncoder, hdrFrame)
            tonemapPassEncoder.end()
            tonemapPassEncoder.release()

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

        onRelease {
            hdrFrame.release()
            hdrTexture.release()
            tonemapper.release()
            depthTexture.release()
            shadowBatch.release()
            shadowMap.release()
        }
    }
}
