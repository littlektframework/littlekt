package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfModelPbrConfig
import com.littlekt.file.gltf.GltfModelUnlitConfig
import com.littlekt.file.gltf.toModel
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readGltf
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
import com.littlekt.util.seconds

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SimpleGltfExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnShiftEsc()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 25f, 150f)
        val environment = UnlitEnvironment(device)
        val pbrEnvironment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(color = Color(0.2f, 0.2f, 0.2f), intensity = 0.1f)
                )
                setAmbientLight(AmbientLight(color = Color(0.002f, 0.002f, 0.002f)))

                addPointLight(PointLight(Vec3f(0f, 250f, 0f), color = Color.GREEN))
                addPointLight(PointLight(Vec3f(0f, 250f, 0f), color = Color.GREEN))
            }

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

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
        val unlitModels =
            listOf(
                resourcesVfs["models/Duck.glb"]
                    .readGltf()
                    .toModel(config = GltfModelUnlitConfig())
                    .apply {
                        scale(20f)
                        translate(-30f, 0f, 0f)
                    },
                resourcesVfs["models/Fox.glb"]
                    .readGltf()
                    .toModel(config = GltfModelUnlitConfig())
                    .apply { translate(30f, 0f, 0f) },
            )

        val pbrModels =
            listOf(
                resourcesVfs["models/flighthelmet/FlightHelmet.gltf"]
                    .readGltf()
                    .toModel(config = GltfModelPbrConfig())
                    .apply {
                        scale(200f)
                        translate(90f, 0f, 0f)
                    }
            )

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
                        format = preferredFormat,
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = AddressMode.REPEAT,
                                addressModeV = AddressMode.REPEAT,
                            ),
                    )
            )

        val grid = run {
            val mesh =
                fullIndexedMesh().generate {
                    vertexModFun = { uv.set(position.x / 100f, position.z / 100f) }
                    grid {
                        sizeX = 1000f
                        sizeY = 1000f
                    }
                }
            ModelInstance(
                    Model(
                        listOf(
                            MeshPrimitive(
                                mesh,
                                PBRMaterial(
                                    device,
                                    baseColorTexture = checkered,
                                    normalTexture = checkeredNormal,
                                    castShadows = false,
                                ),
                            )
                        )
                    )
                )
                .apply { translate(0f, -30f, 0f) }
        }

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                this.depthFormat = depthFormat
            }

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
        }

        addFlyController(camera, 0.5f)
        onUpdate { dt ->
            unlitModels.forEach { model ->
                model.rotate(y = 10.degrees * dt.seconds)
                model.update(dt)
            }

            pbrModels.forEach { model ->
                model.rotate(y = 10.degrees * dt.seconds)
                model.update(dt)
            }
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

            unlitModels.forEach { model -> modelBatch.render(model, environment) }
            pbrModels.forEach { model -> modelBatch.render(model, pbrEnvironment) }
            modelBatch.render(grid, pbrEnvironment)
            modelBatch.flush(renderPassEncoder, camera, dt)
            renderPassEncoder.end()
            renderPassEncoder.release()

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
