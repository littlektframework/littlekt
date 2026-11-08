package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.webgpu.*
import com.littlekt.renderFrame
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
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
        configureSurfaceOnResize()
        val device = graphics.device

        val camera = OrthographicCamera()
        camera.ortho(graphics.width, graphics.height)

        val amount = 100
        val cache = SpriteCache(device, graphics.preferredFormat, size = amount * amount)
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
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height -> camera.ortho(width, height) }

        addWASDMovement(camera, 0.5f)
        onUpdate {
            renderFrame { frame, commandEncoder ->
                camera.update()
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                cache.render(renderPass, camera.viewProjection)
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease { cache.release() }
    }
}
