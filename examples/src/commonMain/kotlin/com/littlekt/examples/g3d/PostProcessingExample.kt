package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.ui.Control
import com.littlekt.graph.node.ui.column
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Bloom
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.Tonemapper
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.postprocess.ChromaticAberrationEffect
import com.littlekt.graphics.postprocess.ColorAdjustEffect
import com.littlekt.graphics.postprocess.FxaaEffect
import com.littlekt.graphics.postprocess.PostProcessor
import com.littlekt.graphics.postprocess.ShaderPostEffect
import com.littlekt.graphics.postprocess.VignetteEffect
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.util.datastructure.fastForEach

/**
 * An example of a [PostProcessor]. The bloom scene is rendered into an HDR texture, bloom is added
 * on top, and then a chain of effects runs into the surface: a [Tonemapper], FXAA, a vignette,
 * color adjustments, chromatic aberration, and a custom pixelate [ShaderPostEffect]. Number keys
 * toggle each effect.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class PostProcessingExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val hdrTarget =
            renderTarget(
                TextureFormat.RGBA16_FLOAT,
                usage = TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
            )
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 3f, 10f)
        camera.rotate((-12).degrees, Vec3f.X_AXIS)

        val colors =
            listOf(
                Color(1f, 0.2f, 0.1f, 1f),
                Color(1f, 0.6f, 0.1f, 1f),
                Color(0.2f, 1f, 0.3f, 1f),
                Color(0.2f, 0.5f, 1f, 1f),
                Color(0.8f, 0.3f, 1f, 1f),
            )
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(DirectionalLight(intensity = 0f))
                setAmbientLight(AmbientLight(Color(0.03f, 0.03f, 0.035f)))
                colors.forEachIndexed { i, color ->
                    addPointLight(
                        PointLight(Vec3f((i - 2) * 2.5f, 1f, 0.8f), color, intensity = 4f, range = 6f)
                    )
                }
            }

        val bloom = Bloom(device, hdrTarget.format)
        val tonemapper = Tonemapper(device, preferredFormat)
        val fxaa = FxaaEffect(device)
        val vignette = VignetteEffect(device)
        val colorAdjust =
            ColorAdjustEffect(device).apply {
                contrast = 1.2f
                saturation = 0.4f
                enabled = false
            }
        val chromatic = ChromaticAberrationEffect(device).apply { enabled = false }
        val pixelate =
            ShaderPostEffect(
                    device,
                    """
                    fn effect(uv: vec2f) -> vec4f {
                        let size = post.params[0].x * post.texel;
                        return textureSample(source, source_sampler, (floor(uv / size) + 0.5) * size);
                    }
                    """,
                    paramCount = 1,
                    label = "pixelate effect",
                )
                .apply {
                    setParam(0, 6f)
                    enabled = false
                }
        val effects = listOf(tonemapper, fxaa, vignette, colorAdjust, chromatic, pixelate)
        val names = listOf("Tonemapper", "FXAA", "Vignette", "Color adjust", "Chromatic aberration", "Pixelate")
        val keys = listOf(Key.NUM1, Key.NUM2, Key.NUM3, Key.NUM4, Key.NUM5, Key.NUM6)
        val postProcessor = PostProcessor(device).apply { this.effects += effects }

        val nodes = mutableListOf<Node3D>()
        val sphere = fullIndexedMesh().generate { uvSphere { radius = 0.6f } }
        val sphereMaterials = mutableListOf<PBRMaterial>()
        colors.forEachIndexed { i, color ->
            val strength = (1 shl i).toFloat()
            val material =
                PBRMaterial(
                    device,
                    metallicFactor = 0f,
                    roughnessFactor = 0.5f,
                    baseColorTexture = Textures.textureWhite,
                    baseColorFactor = Color(0.05f, 0.05f, 0.05f, 1f),
                    emissiveFactor =
                        Vec3f(color.r * strength, color.g * strength, color.b * strength),
                )
            sphereMaterials += material
            nodes +=
                MeshNode(listOf(MeshPrimitive(sphere, material))).apply {
                    translate((i - 2) * 2.5f, 1f, 0f)
                }
        }

        val checkered =
            resourcesVfs["checkered.png"].readTexture(
                options =
                    TextureOptions(
                        format = preferredFormat.toSRGB(),
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = AddressMode.REPEAT,
                                addressModeV = AddressMode.REPEAT,
                            ),
                    )
            )
        val floorMesh =
            fullIndexedMesh().generate {
                vertexModFun = { uv.set(position.x, position.z) }
                grid {
                    sizeX = 30f
                    sizeY = 30f
                }
            }
        val floorMaterial = PBRMaterial(device, metallicFactor = 0f, baseColorTexture = checkered)
        nodes += MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
            }

        val graph =
            sceneGraph(this) {
                    column {
                        anchor(Control.AnchorLayout.TOP_LEFT)
                        marginLeft = 10f
                        effects.forEachIndexed { i, effect ->
                            label {
                                var shown: Boolean? = null
                                onUpdate {
                                    if (shown != effect.enabled) {
                                        shown = effect.enabled
                                        text = "${i + 1}: ${names[i]} ${if (effect.enabled) "on" else "off"}"
                                    }
                                }
                            }
                        }
                    }
                }
                .also { it.initialize() }

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
            graph.resize(width, height)
        }

        addFlyController(camera, 0.005f)

        onUpdate { dt ->
            keys.forEachIndexed { i, key ->
                if (input.isKeyJustPressed(key)) effects[i].enabled = !effects[i].enabled
            }
            nodes.fastForEach { it.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("Bloom Command Encoder") { frame, commandEncoder ->
                camera.update()
                val scenePass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(hdrTarget.view, Color.BLACK),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Scene Render Pass",
                        )
                    )
                nodes.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(scenePass, camera, dt)
                scenePass.end()
                scenePass.release()

                bloom.render(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)
                postProcessor.process(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)

                val tonemapPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame),
                            label = "Post Process Render Pass",
                        )
                    )
                postProcessor.render(tonemapPass, preferredFormat)
                tonemapPass.end()
                tonemapPass.release()

                graph.update(dt)
                graph.render(
                    commandEncoder,
                    RenderPassDescriptor(colorAttachment(frame), label = "UI Render Pass"),
                )
            }
        }

        onRelease {
            graph.release()
            modelBatch.release()
            environment.release()
            bloom.release()
            postProcessor.release()
            effects.fastForEach { it.release() }
            sphere.release()
            sphereMaterials.fastForEach { it.release() }
            floorMesh.release()
            floorMaterial.release()
            checkered.release()
        }
    }
}
