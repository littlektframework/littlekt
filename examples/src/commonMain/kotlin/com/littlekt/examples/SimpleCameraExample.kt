package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.util.viewport.ExtendViewport
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceTextureStatus
import io.ygdrasil.webgpu.TextureUsage

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SimpleCameraExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val logoTexture = resourcesVfs["logo.png"].readTexture()

        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
            graphics.surface.supportedAlphaMode.first()
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ExtendViewport(graphics.width, graphics.height)
        val camera = viewport.camera

        onResize { width, height ->
            viewport.update(width, height, true)
            graphics.configureSurface(
                setOf(TextureUsage.RenderAttachment),
                preferredFormat,
                graphics.surface.supportedAlphaMode.first()
            )
        }

        addWASDMovement(camera, 0.5f)
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
                                loadOp = LoadOp.Clear,
                                storeOp = StoreOp.Store,
                                clearValue = Color.DARK_GRAY.toWebGPUColor()
                            )
                        )
                    )
                )
            camera.update()
            batch.begin()
            batch.draw(logoTexture, 0f, 0f)
            batch.flush(renderPassEncoder, camera.viewProjection)
            batch.end()
            renderPassEncoder.end()

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
            logoTexture.release()
        }
    }
}
