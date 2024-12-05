package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*

/**
 * An example using a [SpriteBatch] to render multiple textures.
 *
 * @author Colton Daily
 * @date 4/12/2024
 */
class MultipleTexturesExample(context: Context) : ContextListener(context) {

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
        val camera = OrthographicCamera()
        camera.ortho(graphics.width, graphics.height)
        camera.position.x = 0f

        onResize { width, height ->
            camera.ortho(width, height)
            camera.position.x = 0f
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        addWASDMovement(camera, 0.5f)
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
                                    clearColor =
                                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                                        else Color.DARK_GRAY,
                                )
                            )
                        )
                )
            camera.update()
            batch.begin()
            batch.draw(logoTexture, -graphics.width * 0.5f, 0f, scaleX = 0.1f, scaleY = 0.1f)
            batch.draw(pikaTexture, graphics.width * 0.5f - pikaTexture.width, 0f)

            batch.flush(renderPassEncoder, camera.viewProjection)
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
