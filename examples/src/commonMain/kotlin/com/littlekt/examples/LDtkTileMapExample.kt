package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.util.viewport.ExtendViewport
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.PresentMode
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceTextureStatus
import io.ygdrasil.webgpu.TextureUsage

/**
 * Load and render an LDtk map.
 *
 * @author Colton Daily
 * @date 4/16/2024
 */
class LDtkTileMapExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device

        val mapLoader = resourcesVfs["ldtk/sample-1.0.ldtk"].readLDtkMapLoader()
        // we are only loading the first level
        // we can load additional levels by doing:
        // mapLoader.loadLevel(levelIdx)
        val world = mapLoader.loadMap()
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
            PresentMode.fifo,
            graphics.surface.supportedAlphaMode.first()
        )

        val batch = SpriteBatch(device, graphics, preferredFormat, size = 400)
        val viewport = ExtendViewport(270, 135)
        val camera = viewport.camera
        val bgColor = world.defaultLevelBackgroundColor

        onResize { width, height ->
            viewport.update(width, height)
            graphics.configureSurface(
                setOf(TextureUsage.RenderAttachment),
                preferredFormat,
                PresentMode.fifo,
                graphics.surface.supportedAlphaMode.first()
            )
        }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                SurfaceTextureStatus.success -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                SurfaceTextureStatus.timeout,
                SurfaceTextureStatus.outdated,
                SurfaceTextureStatus.lost -> {
                    surfaceTexture.texture.close()
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

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(
                                RenderPassDescriptor.ColorAttachment(
                                    view = frame,
                                    loadOp = LoadOp.clear,
                                    storeOp = StoreOp.store,
                                    clearValue = bgColor.toWebGPUColor()
                                )
                            )
                        )
                )
            camera.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                world.render(it, camera, scale = 1f)
            }
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(listOf(commandBuffer))
            graphics.surface.present()

            commandBuffer.close()
            commandEncoder.close()
            frame.close()
            swapChainTexture.close()
        }

        onRelease {
            batch.release()
            mapLoader.release()
        }
    }
}
