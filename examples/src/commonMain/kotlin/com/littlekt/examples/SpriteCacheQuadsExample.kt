package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.resources.Textures
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceTextureStatus
import io.ygdrasil.webgpu.TextureUsage
import kotlin.random.Random

/**
 * An example showing many quads, using instancing, in a single draw call.
 *
 * @author Colton Daily
 * @date 4/20/2024
 */
class SpriteCacheQuadsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val queue = device.queue
        val preferredFormat = graphics.preferredFormat

        val camera = OrthographicCamera()
        camera.ortho(graphics.width, graphics.height)

        val amount = 100
        val cache = SpriteCache(device, preferredFormat, size = amount * amount)
        val tileSize = 16f
        repeat(amount) { y ->
            repeat(amount) { x ->
                cache.add(Textures.white) {
                    position.set(x * tileSize, y * tileSize)
                    size.set(tileSize, tileSize)
                    color.set(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
                }
            }
        }
        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
            graphics.surface.supportedAlphaMode.first()
        )

        onResize { width, height ->
            camera.ortho(width, height)
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
            camera.update()

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
            cache.render(renderPassEncoder, camera.viewProjection)
            renderPassEncoder.end()

            val commandBuffer = commandEncoder.finish()

            queue.submit(listOf(commandBuffer))
            graphics.surface.present()

            commandBuffer.close()
            commandEncoder.close()
            frame.close()
            swapChainTexture.close()
        }

        onRelease { cache.release() }
    }
}
