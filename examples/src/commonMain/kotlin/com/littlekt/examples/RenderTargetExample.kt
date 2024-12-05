package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*

/**
 * An example of rendering to a texture.
 *
 * @author Colton Daily
 * @date 5/9/2024
 */
class RenderTargetExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val device = graphics.device

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val target = EmptyTexture(device, preferredFormat, 256, 256)

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        onResize { _, _ ->
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
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

            val commandEncoder = device.createCommandEncoder("scenegraph command encoder")
            val renderTargetRenderPass =
                commandEncoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = target.view,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                                clearColor =
                                    if (preferredFormat.srgb) Color.YELLOW.toLinear()
                                    else Color.YELLOW,
                            )
                        ),
                        label = "Surface render pass",
                    )
                )

            batch.begin()
            batch.draw(icon, 0f, 0f)
            batch.flush(renderTargetRenderPass)
            renderTargetRenderPass.end()
            renderTargetRenderPass.release()

            val surfaceRenderPass =
                commandEncoder.beginRenderPass(
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
                        label = "Surface render pass",
                    )
                )
            batch.draw(
                target,
                0f,
                0f,
                originX = target.width * 0.5f,
                originY = target.height * 0.5f,
            )
            batch.flush(surfaceRenderPass)
            batch.end()
            surfaceRenderPass.end()
            surfaceRenderPass.release()

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
