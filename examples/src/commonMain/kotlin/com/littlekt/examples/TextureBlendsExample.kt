package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*

/**
 * An example showing drawing textures with different blending.
 *
 * @author Colton Daily
 * @date 4/12/2024
 */
class TextureBlendsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        val device = graphics.device
        val logoTexture = resourcesVfs["logo.png"].readTexture()
        val pikaTexture = resourcesVfs["pika.png"].readTexture()

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)

        onResize { width, height ->
            batch.viewProjection =
                batch.viewProjection.setToOrthographic(
                    left = -width * 0.5f,
                    right = width * 0.5f,
                    bottom = -height * 0.5f,
                    top = height * 0.5f,
                    near = 0f,
                    far = 1f,
                )
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        onUpdate {
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
                                    clearColor = Color.DARK_GRAY,
                                )
                            )
                        )
                )
            batch.begin()
            batch.draw(logoTexture, -graphics.width * 0.5f, 0f, scaleX = 0.1f, scaleY = 0.1f)
            batch.draw(pikaTexture, -graphics.width * 0.5f, -pikaTexture.height - 10f)
            batch.setBlendState(BlendState.Darken)
            batch.draw(logoTexture, 0f, 0f, scaleX = 0.1f, scaleY = 0.1f)
            batch.draw(pikaTexture, 0f, -pikaTexture.height - 10f)
            batch.swapToPreviousBlendState()
            batch.draw(
                logoTexture,
                graphics.width * 0.5f - logoTexture.width * 0.5f * 0.1f,
                0f,
                scaleX = 0.1f,
                scaleY = 0.1f,
            )
            batch.draw(
                pikaTexture,
                graphics.width * 0.5f - pikaTexture.width * 0.5f,
                -pikaTexture.height - 10f,
            )
            batch.flush(renderPassEncoder)
            batch.end()
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

        onRelease {
            batch.release()
            logoTexture.release()
        }
    }
}
