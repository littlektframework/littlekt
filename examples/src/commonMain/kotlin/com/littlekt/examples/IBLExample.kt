package com.littlekt.examples

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
        this.addCloseOnEsc()
        val device = graphics.device
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val hdrFormat = TextureFormat.RGBA16_FLOAT
        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

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
        val skybox = Skybox(device, environmentMap, hdrFormat, depthFormat)
        val tonemapper = Tonemapper(device, preferredFormat)

        val nodes = mutableListOf<Node3D>()
        val sphere = fullIndexedMesh().generate { uvSphere { radius = 0.8f } }
        for (row in 0 until 2) {
            for (column in 0 until 5) {
                nodes +=
                    MeshNode(
                            listOf(
                                MeshPrimitive(
                                    sphere,
                                    PBRMaterial(
                                        device,
                                        metallicFactor = row.toFloat(),
                                        roughnessFactor = 0.05f + column * 0.2375f,
                                        baseColorTexture = Textures.textureWhite,
                                        baseColorFactor =
                                            if (row == 0) Color(0.8f, 0.1f, 0.1f, 1f)
                                            else Color(1f, 0.85f, 0.55f, 1f),
                                    ),
                                )
                            )
                        )
                        .apply { translate((column - 2) * 2f, 1f + row * 2f, -3f) }
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
                colorFormat = hdrFormat
                this.depthFormat = depthFormat
            }

        var hdrTexture = createTarget(device, graphics.width, graphics.height, hdrFormat)
        var hdrFrame = hdrTexture.createView()
        var depthTexture = createTarget(device, graphics.width, graphics.height, depthFormat)
        var depthFrame = depthTexture.createView()

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
            hdrFrame.release()
            hdrTexture.release()
            hdrTexture = createTarget(device, width, height, hdrFormat)
            hdrFrame = hdrTexture.createView()
            depthFrame.release()
            depthTexture.release()
            depthTexture = createTarget(device, width, height, depthFormat)
            depthFrame = depthTexture.createView()
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

            val commandEncoder = device.createCommandEncoder("IBL Command Encoder")
            val scenePass =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = hdrFrame,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                                clearColor = Color.BLACK,
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
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = frame,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                            )
                        ),
                        label = "Tonemap Render Pass",
                    )
                )
            tonemapper.render(tonemapPass, hdrFrame)
            tonemapPass.end()
            tonemapPass.release()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease {
            modelBatch.release()
            environment.release()
            skybox.release()
            environmentMap.release()
            tonemapper.release()
            hdrFrame.release()
            hdrTexture.release()
            depthFrame.release()
            depthTexture.release()
        }
    }

    private fun createTarget(device: Device, width: Int, height: Int, format: TextureFormat) =
        device.createTexture(
            TextureDescriptor(
                Extent3D(width, height, 1),
                1,
                1,
                TextureDimension.D2,
                format,
                if (format.hdr) TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE
                else TextureUsage.RENDER_ATTACHMENT,
            )
        )
}
