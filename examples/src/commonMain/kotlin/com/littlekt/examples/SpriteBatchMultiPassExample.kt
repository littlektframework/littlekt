package com.littlekt.examples

import com.littlekt.AssetProvider
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.ldtk.LDtkMapLoader
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkWorld
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 7/8/2024
 */
class SpriteBatchMultiPassExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val assets = AssetProvider(this)

        val pixelFont: BitmapFont by assets.load(resourcesVfs["m5x7_32.fnt"])
        val mapLoader: LDtkMapLoader by assets.load(resourcesVfs["ldtk/world-1.5.3.ldtk"])
        val world: LDtkWorld by assets.prepare { mapLoader.loadMap(0) }

        val device = graphics.device

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ExtendViewport(270, 135)
        val camera = viewport.camera
        val uiViewport = ExtendViewport(960, 540)
        val uiCam = uiViewport.camera

        onResize { width, height ->
            viewport.update(width, height)
            uiViewport.update(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        addWASDMovement(camera, 0.05f)

        onUpdate {
            if (!assets.fullyLoaded) {
                assets.update()
                return@onUpdate
            }

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
                            )
                        )
                )
            camera.update()
            uiCam.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                world.levels[0].render(batch, camera, 0f, 0f)
                batch.viewProjection = uiCam.viewProjection
                pixelFont.draw(batch, "Hello\nLittleKt!", 0f, 0f, align = HAlign.CENTER)
            }
            renderPassEncoder.end()
            renderPassEncoder.release()

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
