package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
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
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of a directional light casting shadows. The scene is rendered into a
 * [DirectionalShadowMap] first using a [ShadowMaterialPipelineProvider] and then rendered normally
 * with a [PBREnvironment] that samples the shadow map.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class ShadowsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        this.addCloseOnEsc()
        val device = graphics.device
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 6f, 18f)
        camera.rotate((-20).degrees, Vec3f.X_AXIS)

        val shadowMap = DirectionalShadowMap(device).apply { radius = 16f }
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
        val floor =
            MeshNode(
                listOf(
                    MeshPrimitive(
                        fullIndexedMesh().generate {
                            vertexModFun = { uv.set(position.x, position.z) }
                            grid {
                                sizeX = 40f
                                sizeY = 40f
                            }
                        },
                        PBRMaterial(
                            device,
                            baseColorTexture = checkered,
                            normalTexture = checkeredNormal,
                            castShadows = false,
                        ),
                    )
                )
            )

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

        var msaaTexture = createAttachment(device, graphics.width, graphics.height, preferredFormat)
        var msaaFrame = msaaTexture.createView()
        var depthTexture = createAttachment(device, graphics.width, graphics.height, depthFormat)
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
            msaaFrame.release()
            msaaTexture.release()
            msaaTexture = createAttachment(device, width, height, preferredFormat)
            msaaFrame = msaaTexture.createView()
            depthFrame.release()
            depthTexture.release()
            depthTexture = createAttachment(device, width, height, depthFormat)
            depthFrame = depthTexture.createView()
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
            shadowMap.update(lightDirection)

            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()
            val clearColor =
                if (preferredFormat.srgb) Color.DARK_GRAY.toLinear() else Color.DARK_GRAY

            val commandEncoder = device.createCommandEncoder("Shadows Command Encoder")

            // pass 1: render the shadow casters from the light into the shadow map
            val shadowPass = shadowMap.beginRenderPass(commandEncoder)
            models.fastForEach { shadowBatch.render(it, shadowMap.camera, shadowMap.environment) }
            shadowBatch.flush(shadowPass, shadowMap.camera, dt)
            shadowPass.end()
            shadowPass.release()

            // pass 2: render the scene, sampling the shadow map
            val mainPass =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = msaaFrame,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.DISCARD,
                                clearColor = clearColor,
                                resolveTarget = frame,
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
                        label = "Main Render Pass",
                    )
                )
            modelBatch.render(floor, environment)
            models.fastForEach { modelBatch.render(it, camera, environment) }
            modelBatch.flush(mainPass, camera, dt)
            mainPass.end()
            mainPass.release()

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
            shadowBatch.release()
            environment.release()
            shadowMap.release()
            msaaFrame.release()
            msaaTexture.release()
            depthFrame.release()
            depthTexture.release()
        }
    }

    private fun createAttachment(device: Device, width: Int, height: Int, format: TextureFormat) =
        device.createTexture(
            TextureDescriptor(
                Extent3D(width, height, 1),
                1,
                SAMPLE_COUNT,
                TextureDimension.D2,
                format,
                TextureUsage.RENDER_ATTACHMENT,
            )
        )

    companion object {
        private const val SAMPLE_COUNT = 4
    }
}
