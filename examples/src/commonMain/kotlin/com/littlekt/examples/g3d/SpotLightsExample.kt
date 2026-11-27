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
import com.littlekt.graphics.Tonemapper
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.SpotLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.shadow.ShadowMaterialPipelineProvider
import com.littlekt.graphics.g3d.shadow.SpotShadowMap
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.postprocess.PostProcessor
import com.littlekt.graphics.postprocess.ShaderPostEffect
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of spot lights casting shadows into a [SpotShadowMap] in a dark scene, with a
 * flashlight that follows the camera and distance fog that reads the scene depth in a
 * [ShaderPostEffect]. Press `F` to toggle the flashlight.
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
class SpotLightsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val hdrTarget = renderTarget(TextureFormat.RGBA16_FLOAT)
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.far = 100f
        camera.translate(3f, 7f, 5f)
        camera.rotate((-50).degrees, Vec3f.X_AXIS)

        val flashlight =
            SpotLight(
                color = Color(1f, 0.92f, 0.8f),
                intensity = 40f,
                range = 25f,
                innerAngle = 12.degrees,
                outerAngle = 22.degrees,
                castShadows = true,
            )
        val lamps =
            listOf(
                SpotLight(
                    position = Vec3f(-4f, 5f, 0f),
                    direction = Vec3f.NEG_Y_AXIS,
                    color = Color(1f, 0.55f, 0.25f),
                    intensity = 80f,
                    range = 12f,
                    innerAngle = 20.degrees,
                    outerAngle = 35.degrees,
                    castShadows = true,
                ),
                SpotLight(
                    position = Vec3f(4f, 5f, -2f),
                    direction = Vec3f.NEG_Y_AXIS,
                    color = Color(0.35f, 0.5f, 1f),
                    intensity = 80f,
                    range = 12f,
                    innerAngle = 15.degrees,
                    outerAngle = 30.degrees,
                    castShadows = true,
                ),
            )

        val spotShadowMap = SpotShadowMap(device, count = 3)
        val environment =
            PBREnvironment(device, spotShadowMap = spotShadowMap).apply {
                setDirectionalLight(DirectionalLight(intensity = 0f))
                setAmbientLight(AmbientLight(Color(0.004f, 0.004f, 0.006f)))
                addSpotLight(flashlight)
                lamps.fastForEach { addSpotLight(it) }
            }

        val fog =
            ShaderPostEffect(
                    device,
                    """
                    fn effect(uv: vec2f) -> vec4f {
                        let color = textureSample(source, source_sampler, uv);
                        let distance = post_linear_depth(post_load_depth_0(uv));
                        let fog = clamp(1.0 - exp(-distance * post.params[0].x), 0.0, 1.0);
                        return vec4f(mix(color.rgb, post.params[1].xyz, fog), color.a);
                    }
                    """,
                    paramCount = 7,
                    label = "fog effect",
                    depthTextureCount = 1,
                )
                .apply {
                    setParam(0, 0.05f)
                    setParam(4, 0.002f)
                    setParam(5, 0.002f)
                    setParam(6, 0.003f)
                }
        val tonemapper = Tonemapper(device, preferredFormat)
        val postProcessor = PostProcessor(device).apply { effects += listOf(fog, tonemapper) }

        val models = mutableListOf<Node3D>()
        val duck = resourcesVfs["models/Duck.glb"].readGltfModel(config = GltfLoaderPbrConfig())
        for (i in 0 until 6) {
            val angle = i * (PI.toFloat() * 2f / 6f)
            models +=
                duck.copy().apply {
                    translate(cos(angle) * 4f, 0f, sin(angle) * 4f - 1f)
                    rotate(y = (i * 60).degrees)
                }
        }
        val fox =
            resourcesVfs["models/Fox.glb"]
                .readGltfModel(
                    config = GltfLoaderPbrConfig(GltfModelConfig(castShadows = true, skinned = true))
                )
                .apply {
                    scale(0.02f)
                    translate(0f, 0f, -1f)
                    enableAnimation(1)
                }
        models += fox

        val pillarMesh = fullIndexedMesh().generate { cube { origin.set(-0.5f, 0f, -0.5f) } }
        val pillarMaterial =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.8f,
                baseColorTexture = Textures.textureWhite,
                baseColorFactor = Color(0.6f, 0.6f, 0.6f, 1f),
            )
        for (i in 0 until 4) {
            models +=
                MeshNode(listOf(MeshPrimitive(pillarMesh, pillarMaterial))).apply {
                    scale(0.6f, 4f, 0.6f)
                    translate(-6f + i * 4f, 0f, -6f)
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
                    sizeX = 40f
                    sizeY = 40f
                }
            }
        val floorMaterial =
            PBRMaterial(device, metallicFactor = 0f, baseColorTexture = checkered, castShadows = false)
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
            }
        val shadowBatch =
            ModelBatch(device).apply {
                defaultPipelineProvider = ShadowMaterialPipelineProvider()
                depthFormat = spotShadowMap.format
            }

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.005f)

        var time = 0f
        onUpdate { dt ->
            time += dt.seconds
            if (input.isKeyJustPressed(Key.F)) {
                flashlight.intensity = if (flashlight.intensity > 0f) 0f else 40f
            }
            lamps.forEachIndexed { i, lamp ->
                val swing = time * (0.8f + i * 0.3f)
                lamp.direction.set(sin(swing) * 0.5f, -1f, cos(swing * 0.7f) * 0.3f)
            }
            models.fastForEach { model ->
                if (model !== fox) model.rotate(y = 10.degrees * dt.seconds)
                model.update(dt)
            }
            floor.update(dt)
        }

        onUpdate { dt ->
            renderFrame("Spot Lights Command Encoder") { frame, commandEncoder ->
                camera.update()
                flashlight.position
                    .set(camera.position)
                    .add(camera.right.x * 0.25f, camera.right.y * 0.25f, camera.right.z * 0.25f)
                    .add(camera.up.x * -0.2f, camera.up.y * -0.2f, camera.up.z * -0.2f)
                flashlight.direction.set(camera.direction)

                spotShadowMap.update(environment.spotLights)
                spotShadowMap.render(commandEncoder, shadowBatch, models, dt)

                val scenePass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(hdrTarget.view, Color.BLACK),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Scene Render Pass",
                        )
                    )
                modelBatch.render(floor, camera, environment)
                models.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(scenePass, camera, dt)
                scenePass.end()
                scenePass.release()

                fog.setDepthTexture(0, depthTarget.depthView)
                fog.setCamera(camera)
                postProcessor.process(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)
                val postPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame), label = "Post Process Render Pass")
                    )
                postProcessor.render(postPass, preferredFormat)
                postPass.end()
                postPass.release()
            }
        }

        onRelease {
            duck.release()
            fox.release()
            modelBatch.release()
            shadowBatch.release()
            environment.release()
            spotShadowMap.release()
            postProcessor.release()
            fog.release()
            tonemapper.release()
            pillarMesh.release()
            pillarMaterial.release()
            floorMesh.release()
            floorMaterial.release()
            checkered.release()
        }
    }
}
