package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.file.gltf.GltfModelConfig
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.shadow.DirectionalShadowMap
import com.littlekt.graphics.g3d.shadow.ShadowMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.surfaceColor
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of a directional light casting shadows. The scene is rendered into each cascade of a
 * [DirectionalShadowMap] first using a [ShadowMaterialPipelineProvider] and then rendered normally
 * with a [PBREnvironment] that samples the shadow map.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class ShadowsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 6f, 18f)
        camera.rotate((-20).degrees, Vec3f.X_AXIS)

        val shadowMap = DirectionalShadowMap(device, cascadeCount = 3).apply { shadowDistance = 40f }
        val lightDirection = MutableVec3f(0.5f, 0.8f, 0.3f).norm()
        val environment =
            PBREnvironment(device, shadowMap = shadowMap).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(1f, 0.95f, 0.85f),
                        direction = lightDirection,
                        intensity = 2.5f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.05f, 0.05f, 0.06f)))
            }

        val models = mutableListOf<Node3D>()
        val duck = resourcesVfs["models/Duck.glb"].readGltfModel(config = GltfLoaderPbrConfig())
        for (i in 0 until 8) {
            val angle = i * (PI.toFloat() * 2f / 8f)
            models +=
                duck.copy().apply {
                    translate(cos(angle) * 7f, 0f, sin(angle) * 7f)
                    rotate(y = (i * 45).degrees)
                }
        }
        val fox =
            resourcesVfs["models/Fox.glb"]
                .readGltfModel(
                    config = GltfLoaderPbrConfig(GltfModelConfig(castShadows = true, skinned = true))
                )
                .apply {
                    scale(0.03f)
                    enableAnimation(1)
                }
        models += fox

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
        val floorMesh =
            fullIndexedMesh().generate {
                vertexModFun = { uv.set(position.x, position.z) }
                grid {
                    sizeX = 40f
                    sizeY = 40f
                }
            }
        val floorMaterial =
            PBRMaterial(
                device,
                baseColorTexture = checkered,
                normalTexture = checkeredNormal,
                castShadows = false,
            )
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                this.depthFormat = depthFormat
                sampleCount = SAMPLE_COUNT
            }
        val shadowBatch =
            ModelBatch(device).apply {
                defaultPipelineProvider = ShadowMaterialPipelineProvider()
                this.depthFormat = shadowMap.format
            }

        val msaaTarget = renderTarget(preferredFormat, SAMPLE_COUNT)
        val depthTarget = renderTarget(depthFormat, SAMPLE_COUNT)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.005f)

        var sunAngle = 0f
        onUpdate { dt ->
            sunAngle += dt.seconds * 0.3f
            lightDirection.set(cos(sunAngle) * 0.6f, 0.8f, sin(sunAngle) * 0.6f).norm()
            models.fastForEach { model ->
                if (model !== fox) model.rotate(y = 15.degrees * dt.seconds)
                model.update(dt)
            }
        }

        onUpdate { dt ->
            renderFrame("Shadows Command Encoder") { frame, commandEncoder ->
                camera.update()
                shadowMap.update(lightDirection, camera)

                shadowMap.render(commandEncoder, shadowBatch, models, dt)

                val mainPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = msaaTarget.view,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.DISCARD,
                                    clearColor = clearColor,
                                    resolveTarget = frame,
                                )
                            ),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Main Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                models.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(mainPass, camera, dt)
                mainPass.end()
                mainPass.release()
            }
        }

        onRelease {
            duck.release()
            fox.release()
            modelBatch.release()
            shadowBatch.release()
            environment.release()
            shadowMap.release()
            floorMesh.release()
            floorMaterial.release()
            checkered.release()
            checkeredNormal.release()
        }
    }

    companion object {
        private const val SAMPLE_COUNT = 4
    }
}
