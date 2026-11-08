package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.gltf.GltfLoaderPbrConfig
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g2d.SpriteBatch
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
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import com.littlekt.util.seconds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of rendering the same scene with multiple cameras and render passes in a single frame.
 * The scene is rendered from a top down camera into a texture, then from the main camera, and then
 * the top down render is drawn on top as a minimap. Both 3D passes use 4x MSAA.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class MultiPassExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        this.addCloseOnEsc()
        val device = graphics.device
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(0f, 4f, 16f)
        camera.rotate((-15).degrees, Vec3f.X_AXIS)

        val minimapCamera = PerspectiveCamera(MINIMAP_SIZE, MINIMAP_SIZE)
        minimapCamera.translate(0f, 30f, 0f)
        minimapCamera.rotate((-90).degrees, Vec3f.X_AXIS)

        val uiCamera = OrthographicCamera()
        uiCamera.ortho(graphics.width, graphics.height)

        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(0.4f, 0.4f, 0.4f),
                        direction = Vec3f(0.3f, 0.6f, 0.8f),
                        intensity = 0.3f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.02f, 0.02f, 0.02f)))
            }

        val lightColors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        val lightPositions = List(LIGHT_COUNT) { MutableVec3f() }
        lightPositions.fastForEachWithIndex { index, position ->
            environment.addPointLight(
                PointLight(
                    position = position,
                    color = lightColors[index % lightColors.size],
                    intensity = 4f,
                    range = 8f,
                )
            )
        }

        val duck = resourcesVfs["models/Duck.glb"].readGltfModel(config = GltfLoaderPbrConfig())
        val ducks = mutableListOf<Node3D>()
        for (x in -2..2) {
            for (z in -2..2) {
                ducks +=
                    duck.copy().apply {
                        translate(x * 4f, 0f, z * 4f)
                        rotate(y = ((x + z) * 40).degrees)
                    }
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
        val spriteBatch = SpriteBatch(device, graphics, preferredFormat)

        val minimap = EmptyTexture(device, preferredFormat, MINIMAP_SIZE, MINIMAP_SIZE)
        val minimapMsaaTexture =
            createAttachment(device, MINIMAP_SIZE, MINIMAP_SIZE, preferredFormat)
        val minimapMsaaFrame = minimapMsaaTexture.createView()
        val minimapDepthTexture = createAttachment(device, MINIMAP_SIZE, MINIMAP_SIZE, depthFormat)
        val minimapDepthFrame = minimapDepthTexture.createView()

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
            uiCamera.ortho(width, height)
        }

        addFlyController(camera, 0.005f)

        var lightAngle = 0f
        onUpdate { dt ->
            lightAngle += dt.seconds * 0.5f
            lightPositions.fastForEachWithIndex { index, position ->
                val angle = lightAngle + index * (PI.toFloat() * 2f / LIGHT_COUNT)
                position.set(cos(angle) * 8f, 1.5f, sin(angle) * 8f)
            }
            ducks.fastForEach { model ->
                model.rotate(y = 20.degrees * dt.seconds)
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
            minimapCamera.update()

            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()
            val clearColor =
                if (preferredFormat.srgb) Color.DARK_GRAY.toLinear() else Color.DARK_GRAY

            val commandEncoder = device.createCommandEncoder("Multi Pass Command Encoder")

            // pass 1: render the scene from the top down camera into the minimap texture
            val minimapPass =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = minimapMsaaFrame,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.DISCARD,
                                clearColor = clearColor,
                                resolveTarget = minimap.view,
                            )
                        ),
                        depthStencilAttachment = depthAttachment(minimapDepthFrame),
                        label = "Minimap Render Pass",
                    )
                )
            modelBatch.render(floor, environment)
            ducks.fastForEach { modelBatch.render(it, minimapCamera, environment) }
            modelBatch.flush(minimapPass, minimapCamera, dt)
            minimapPass.end()
            minimapPass.release()

            // pass 2: render the scene from the main camera
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
                        depthStencilAttachment = depthAttachment(depthFrame),
                        label = "Main Render Pass",
                    )
                )
            modelBatch.render(floor, environment)
            ducks.fastForEach { modelBatch.render(it, camera, environment) }
            modelBatch.flush(mainPass, camera, dt)
            mainPass.end()
            mainPass.release()

            // pass 3: draw the minimap on top
            val uiPass =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = frame,
                                loadOp = LoadOp.LOAD,
                                storeOp = StoreOp.STORE,
                            )
                        ),
                        label = "UI Render Pass",
                    )
                )
            spriteBatch.begin(uiCamera.viewProjection)
            spriteBatch.draw(
                minimap,
                uiCamera.virtualWidth - MINIMAP_SIZE - MINIMAP_PADDING,
                uiCamera.virtualHeight - MINIMAP_SIZE - MINIMAP_PADDING,
            )
            spriteBatch.flush(uiPass)
            spriteBatch.end()
            uiPass.end()
            uiPass.release()

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
            spriteBatch.release()
            environment.release()
            minimap.release()
            minimapMsaaFrame.release()
            minimapMsaaTexture.release()
            minimapDepthFrame.release()
            minimapDepthTexture.release()
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

    private fun depthAttachment(view: TextureView) =
        RenderPassDepthStencilAttachmentDescriptor(
            view = view,
            depthClearValue = 1f,
            depthLoadOp = LoadOp.CLEAR,
            depthStoreOp = StoreOp.STORE,
            stencilClearValue = 0,
            stencilLoadOp = LoadOp.CLEAR,
            stencilStoreOp = StoreOp.STORE,
        )

    companion object {
        private const val MINIMAP_SIZE = 256
        private const val MINIMAP_PADDING = 10f
        private const val LIGHT_COUNT = 4
        private const val SAMPLE_COUNT = 4
    }
}
