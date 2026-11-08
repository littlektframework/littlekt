package com.littlekt.examples.g3d

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.file.vfs.readHdrImage
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.Tonemapper
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.ibl.EnvironmentMap
import com.littlekt.graphics.g3d.ibl.Skybox
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.resources.Textures
import com.littlekt.util.datastructure.fastForEach

/**
 * An example of image based lighting (IBL). An [EnvironmentMap] baked from an HDR image lights the
 * PBR materials and is drawn behind the scene with a [Skybox]. The spheres go from smooth to rough,
 * left to right, with dielectric spheres on the bottom row and metallic spheres on the top row.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class IBLExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val hdrTarget =
            renderTarget(
                TextureFormat.RGBA16_FLOAT,
                usage = TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
            )
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 2.5f, 9f)
        camera.rotate((-8).degrees, Vec3f.X_AXIS)

        val environmentMap =
            EnvironmentMap(
                device,
                resourcesVfs["hdri/kloofendal_48d_partly_cloudy_1k.hdr"].readHdrImage(),
            )
        val environment =
            PBREnvironment(device, environmentMap = environmentMap).apply {
                setDirectionalLight(DirectionalLight(intensity = 0f))
                setAmbientLight(AmbientLight(Color.BLACK))
            }
        val skybox = Skybox(device, environmentMap, hdrTarget.format, depthTarget.format)
        val tonemapper = Tonemapper(device, graphics.preferredFormat)

        val nodes = mutableListOf<Node3D>()
        val sphere = fullIndexedMesh().generate { uvSphere { radius = 0.8f } }
        val sphereMaterials = mutableListOf<PBRMaterial>()
        for (row in 0 until 2) {
            for (column in 0 until 5) {
                val material =
                    PBRMaterial(
                        device,
                        metallicFactor = row.toFloat(),
                        roughnessFactor = 0.05f + column * 0.2375f,
                        baseColorTexture = Textures.textureWhite,
                        baseColorFactor =
                            if (row == 0) Color(0.8f, 0.1f, 0.1f, 1f)
                            else Color(1f, 0.85f, 0.55f, 1f),
                    )
                sphereMaterials += material
                nodes +=
                    MeshNode(listOf(MeshPrimitive(sphere, material))).apply {
                        translate((column - 2) * 2f, 1f + row * 2f, -3f)
                    }
            }
        }
        nodes +=
            resourcesVfs["models/flighthelmet/FlightHelmet.gltf"]
                .readGltfModel(config = GltfLoaderPbrConfig())
                .apply {
                    scale(4f)
                    translate(0f, -0.5f, 2f)
                }

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
            }

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.005f)

        onUpdate { dt ->
            if (input.isKeyJustPressed(Key.PAGE_UP)) {
                environmentMap.intensity *= 1.25f
            }
            if (input.isKeyJustPressed(Key.PAGE_DOWN)) {
                environmentMap.intensity /= 1.25f
            }
            nodes.fastForEach { it.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("IBL Command Encoder") { frame, commandEncoder ->
                camera.update()
                val scenePass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(hdrTarget.view, Color.BLACK)),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Scene Render Pass",
                        )
                    )
                nodes.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(scenePass, camera, dt)
                skybox.render(scenePass, camera)
                scenePass.end()
                scenePass.release()

                val tonemapPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(frame)),
                            label = "Tonemap Render Pass",
                        )
                    )
                tonemapper.render(tonemapPass, hdrTarget.view)
                tonemapPass.end()
                tonemapPass.release()
            }
        }

        onRelease {
            modelBatch.release()
            environment.release()
            skybox.release()
            environmentMap.release()
            tonemapper.release()
            sphere.release()
            sphereMaterials.fastForEach { it.release() }
        }
    }
}
