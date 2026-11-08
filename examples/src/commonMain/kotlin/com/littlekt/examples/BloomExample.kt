package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
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
        this.addCloseOnEsc()
        val device = graphics.device
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val hdrFormat = TextureFormat.RGBA16_FLOAT
        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

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

        val bloom = Bloom(device, hdrFormat)
        val tonemapper = Tonemapper(device, preferredFormat)
        var bloomEnabled = true

        val nodes = mutableListOf<Node3D>()
        val sphere = fullIndexedMesh().generate { uvSphere { radius = 0.6f } }
        colors.forEachIndexed { i, color ->
            val strength = (1 shl i).toFloat()
            nodes +=
                MeshNode(
                        listOf(
                            MeshPrimitive(
                                sphere,
                                PBRMaterial(
                                    device,
                                    metallicFactor = 0f,
                                    roughnessFactor = 0.5f,
                                    baseColorTexture = Textures.textureWhite,
                                    baseColorFactor = Color(0.05f, 0.05f, 0.05f, 1f),
                                    emissiveFactor =
                                        Vec3f(color.r * strength, color.g * strength, color.b * strength),
                                ),
                            )
                        )
                    )
                    .apply { translate((i - 2) * 2.5f, 1f, 0f) }
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
        nodes +=
            MeshNode(
                listOf(
                    MeshPrimitive(
                        fullIndexedMesh().generate {
                            vertexModFun = { uv.set(position.x, position.z) }
                            grid {
                                sizeX = 30f
                                sizeY = 30f
                            }
                        },
                        PBRMaterial(device, metallicFactor = 0f, baseColorTexture = checkered),
                    )
                )
            )

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = hdrFormat
                this.depthFormat = depthFormat
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

            val commandEncoder = device.createCommandEncoder("Bloom Command Encoder")
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
            scenePass.end()
            scenePass.release()

            if (bloomEnabled) {
                bloom.render(commandEncoder, hdrFrame, graphics.width, graphics.height)
            }

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

            graph.update(dt)
            graph.render(
                commandEncoder,
                RenderPassDescriptor(
                    listOf(
                        RenderPassColorAttachmentDescriptor(
                            view = frame,
                            loadOp = LoadOp.LOAD,
                            storeOp = StoreOp.STORE,
                        )
                    ),
                    label = "UI Render Pass",
                ),
            )

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
            bloom.release()
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
