package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.resources.Fonts
import com.littlekt.util.viewport.ExtendViewport
import io.ygdrasil.wgpu.LoadOp
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.RenderPassDescriptor
import io.ygdrasil.wgpu.StoreOp
import io.ygdrasil.wgpu.SurfaceTextureStatus
import io.ygdrasil.wgpu.TextureUsage

/**
 * @author Colton Daily
 * @date 4/17/2024
 */
class FontExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val pixelFont = resourcesVfs["m5x7_32.fnt"].readBitmapFont()
        val arialFont = resourcesVfs["arial_32.fnt"].readBitmapFont()
        val device = graphics.device

        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            setOf(TextureUsage.renderattachment),
            preferredFormat,
            PresentMode.fifo,
            graphics.surface.supportedAlphaMode.first()
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        onResize { width, height ->
            viewport.update(width, height)
            graphics.configureSurface(
                setOf(TextureUsage.renderattachment),
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
                                    clearValue = Color.DARK_GRAY.toWebGPUColor()
                                )
                            )
                        )
                )
            camera.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                pixelFont.draw(it, "Hello\nLittleKt!", 0f, 0f, align = HAlign.CENTER)
                arialFont.draw(it, "Hello\nLittleKt!", -300f, 0f)
                Fonts.default.draw(it, "Hello\nLittleKt!", 150f, 0f, align = HAlign.RIGHT)
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

        onRelease { batch.release() }
    }
}
