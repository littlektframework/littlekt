package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.examples.g3d.debug.DepthMaterial
import com.littlekt.examples.g3d.debug.DepthMaterialPipelineProvider
import com.littlekt.examples.g3d.debug.DepthSliceMaterial
import com.littlekt.examples.g3d.debug.DepthSliceMaterialPipelineProvider
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
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.toString

/**
 * An example of PBR rendering of the Sponza glTF scene with cascaded shadows, a directional light,
 * and over a thousand clustered point lights. The scene is rendered into an HDR texture and then
 * tonemapped to the surface. Press 1, 2, or 3 to switch between the normal, depth slice, and depth
 * views.
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
        addCloseOnEsc()
        configureSurfaceOnResize()
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

        val tonemapper = Tonemapper(device, graphics.preferredFormat)
        val clearColor = Color.DARK_GRAY.toLinear()
        val hdrTarget =
            renderTarget(
                TextureFormat.RGBA16_FLOAT,
                usage = TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
            )
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
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
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
            }

        loadGltfModels(models, modelBatch)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()

            graph.resize(width, height)
        }
        var renderView = RenderView.NORMAL

        addFlyController(camera)

        onUpdate {
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

        onUpdate { dt -> models.fastForEach { model -> model.scene?.update(dt) } }

        onUpdate { dt ->
            renderFrame { frame, commandEncoder ->
                camera.update()

                shadowCasters.clear()
                models.fastForEach { model -> model.scene?.let { shadowCasters += it } }
                shadowMap.update(sunDirection, camera)
                shadowMap.render(commandEncoder, shadowBatch, shadowCasters, dt)
                val renderPassEncoder =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(hdrTarget.view, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
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
                            colorAttachment(frame),
                            label = "tonemap render pass",
                        )
                    )
                tonemapper.render(tonemapPassEncoder, hdrTarget.view)
                tonemapPassEncoder.end()
                tonemapPassEncoder.release()

                graph.update(dt)
                graph.render(
                    commandEncoder,
                    RenderPassDescriptor(colorAttachment(frame), label = "ui render pass"),
                )
            }
        }

        onRelease {
            models.fastForEach { it.scene?.release() }
            graph.release()
            modelBatch.release()
            shadowBatch.release()
            shadowMap.release()
            pbrEnvironment.release()
            simpleEnvironment.release()
            depthMaterial.release()
            depthSliceMaterial.release()
            tonemapper.release()
        }
    }
}
