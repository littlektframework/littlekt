package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.graph.node.ui.centerContainer
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example using a render pass for the game world and a render pass for the UI.
 *
 * @author Colton Daily
 * @date 5/2/2024
 */
class GameWorldAndUIViewports(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val mapLoader = resourcesVfs["ldtk/world-1.5.3.ldtk"].readLDtkMapLoader()
        // we are only loading the first level
        // we can load additional levels by doing:
        // mapLoader.loadLevel(levelIdx)
        val world = mapLoader.loadMap(0, translateMapHeight = false)
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0]
        )

        val batch = SpriteBatch(device, graphics, preferredFormat, size = 400)
        val worldViewport = ExtendViewport(270, 135)
        val worldCamera = worldViewport.camera
        addWASDMovement(worldCamera, 0.05f)
        val bgColor =
            if (preferredFormat.srgb) world.defaultLevelBackgroundColor.toLinear()
            else world.defaultLevelBackgroundColor
        val graph =
            sceneGraph(this, viewport = ExtendViewport(960, 540), batch = batch) {
                    centerContainer {
                        anchorRight = 1f
                        anchorTop = 1f
                        label { text = "This is my UI!" }
                    }
                }
                .also { it.initialize() }

        graph.requestShowDebugInfo = true

        onResize { width, height ->
            graph.resize(width, height)
            worldViewport.update(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0]
            )
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
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder("command encoder")

            val worldRenderPass =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor = bgColor
                                )
                            )
                        )
                )
            worldCamera.update()
            batch.begin(worldCamera.viewProjection)
            world.render(batch, worldCamera, scale = 1f)
            batch.flush(worldRenderPass)
            worldRenderPass.end()
            worldRenderPass.release()

            val uiRenderPassDescriptor =
                RenderPassDescriptor(
                    listOf(
                        RenderPassColorAttachmentDescriptor(
                            view = frame,
                            loadOp = LoadOp.LOAD,
                            storeOp = StoreOp.STORE
                        )
                    ),
                    label = "Init render pass"
                )

            graph.update(dt)
            graph.render(commandEncoder, uiRenderPassDescriptor)
            batch.end()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease { batch.release() }
    }
}
