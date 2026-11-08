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
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.toString

/**
 * An example of [Bloom]. Emissive spheres, brighter from left to right, glow once their color goes
 * over the bloom threshold. The scene is rendered into an HDR texture, bloom is added on top, and
 * then it is tonemapped to the surface.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class BloomExample(context: Context) : ContextListener(context) {

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
        var bloomEnabled = true

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
                        label { onUpdate { text = "Bloom (B): ${if (bloomEnabled) "on" else "off"}" } }
                        label {
                            onUpdate {
                                text = "Intensity (PgUp/PgDn): ${bloom.intensity.toString(2)}"
                            }
                        }
                        label {
                            onUpdate {
                                text = "Threshold (Up/Down): ${bloom.threshold.toString(2)}"
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
            if (input.isKeyJustPressed(Key.B)) bloomEnabled = !bloomEnabled
            if (input.isKeyJustPressed(Key.PAGE_UP)) bloom.intensity += 0.1f
            if (input.isKeyJustPressed(Key.PAGE_DOWN)) {
                bloom.intensity = (bloom.intensity - 0.1f).coerceAtLeast(0f)
            }
            if (input.isKeyJustPressed(Key.ARROW_UP)) bloom.threshold += 0.25f
            if (input.isKeyJustPressed(Key.ARROW_DOWN)) {
                bloom.threshold = (bloom.threshold - 0.25f).coerceAtLeast(0f)
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

                if (bloomEnabled) {
                    bloom.render(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)
                }

                val tonemapPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame),
                            label = "Tonemap Render Pass",
                        )
                    )
                tonemapper.render(tonemapPass, hdrTarget.view)
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
            tonemapper.release()
            sphere.release()
            sphereMaterials.fastForEach { it.release() }
            floorMesh.release()
            floorMaterial.release()
            checkered.release()
        }
    }
}
