package com.littlekt.examples.g3d

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.file.gltf.GltfLoaderUnlitConfig
import com.littlekt.file.gltf.GltfModelConfig
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.*
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds

/**
 * An example of loading and rendering glTF models. An unlit duck, an unlit animated fox, and a PBR
 * flight helmet spin on top of a PBR checkered floor lit by a directional light and point lights.
 * Move around with the fly controller.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SimpleGltfExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 1f, 5f)
        val environment = UnlitEnvironment(device)
        val pbrEnvironment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color.WHITE,
                        direction = Vec3f(0.3f, 0.5f, 0.8f),
                        intensity = 0.6f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.02f, 0.02f, 0.02f)))

                addPointLight(PointLight(Vec3f(0f, 1f, 0f), color = Color.GREEN, range = 2f))
                addPointLight(PointLight(Vec3f(0f, 1f, 1f), color = Color.RED, range = 2f))
                addPointLight(PointLight(Vec3f(0f, 1f, -1f), color = Color.BLUE, range = 2f))
            }

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }

        val models =
            listOf(
                GltfModel(
                    resourcesVfs["models/Duck.glb"],
                    GltfLoaderUnlitConfig(),
                    environment,
                    1f,
                    Vec3f(-1f, 0f, 0f),
                ),
                GltfModel(
                    resourcesVfs["models/Fox.glb"],
                    GltfLoaderUnlitConfig(GltfModelConfig(skinned = true)),
                    environment,
                    0.01f,
                    Vec3f(-3f, 0f, 0f),
                    animIdx = 1,
                ),
                GltfModel(
                    resourcesVfs["models/flighthelmet/FlightHelmet.gltf"],
                    GltfLoaderPbrConfig(),
                    pbrEnvironment,
                    4f,
                    Vec3f(1f, 0f, 0f),
                ),
            )

        loadGltfModels(models, modelBatch)

        val checkered =
            resourcesVfs["checkered.png"].readTexture(
                options =
                    TextureOptions(
                        format = preferredFormat,
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = AddressMode.REPEAT,
                                addressModeV = AddressMode.REPEAT,
                            ),
                    )
            )

        val checkeredNormal =
            resourcesVfs["checkered_normal.png"].readTexture(
                options =
                    TextureOptions(
                        format = preferredFormat.toNonSRGB(),
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = AddressMode.REPEAT,
                                addressModeV = AddressMode.REPEAT,
                            ),
                    )
            )

        val gridMesh =
            fullIndexedMesh().generate {
                vertexModFun = { uv.set(position.x, position.z) }
                grid {
                    sizeX = 50f
                    sizeY = 50f
                }
            }
        val gridMaterial =
            PBRMaterial(
                device,
                baseColorTexture = checkered,
                normalTexture = checkeredNormal,
                castShadows = false,
            )
        val grid = MeshNode(listOf(MeshPrimitive(gridMesh, gridMaterial)))
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.0015f)
        onUpdate { dt ->
            models.fastForEach { model ->
                model.scene?.let {
                    it.rotate(y = 10.degrees * dt.seconds)
                    it.update(dt)
                }
            }
        }
        onUpdate { dt ->
            renderFrame("Main Command Encoder") { frame, commandEncoder ->
                camera.update()
                val renderPassEncoder =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(frame, clearColor)),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Main Render Pass",
                        )
                    )

                modelBatch.renderGltfModels(models, camera)
                modelBatch.render(grid, pbrEnvironment)
                modelBatch.flush(renderPassEncoder, camera, dt)
                renderPassEncoder.end()
                renderPassEncoder.release()
            }
        }

        onRelease {
            modelBatch.release()
            environment.release()
            pbrEnvironment.release()
            gridMesh.release()
            gridMaterial.release()
            checkered.release()
            checkeredNormal.release()
        }
    }
}
