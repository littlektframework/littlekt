package com.littlekt.examples.g2d.tilemap

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.renderFrame
import com.littlekt.surfaceColor
import com.littlekt.util.viewport.ExtendViewport

/**
 * Load and render an LDtk world with a [SpriteBatch]. Press C to toggle rendering it with a
 * [SpriteCache] instead. Move the camera with WASD.
 *
 * @author Colton Daily
 * @date 4/16/2024
 */
class LDtkTileMapExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device

        val mapLoader = resourcesVfs["ldtk/sample-1.0.ldtk"].readLDtkMapLoader()
        val world = mapLoader.loadMap()

        val batch = SpriteBatch(device, graphics, graphics.preferredFormat, size = 400)
        val cache = SpriteCache(device, graphics.preferredFormat)
        world.addToCache(cache, scale = 1f)
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val bgColor = graphics.surfaceColor(world.defaultLevelBackgroundColor)
        var useCache = false

        onResize { width, height -> viewport.update(width, height) }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            if (input.isKeyJustPressed(Key.C)) useCache = !useCache
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, bgColor))
                    )
                camera.update()

                if (useCache) {
                    cache.render(renderPass, camera.viewProjection)
                } else {
                    batch.use(renderPass, camera.viewProjection) {
                        world.render(it, camera, scale = 1f)
                    }
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            cache.release()
            mapLoader.release()
        }
    }
}
