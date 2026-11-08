package com.littlekt.examples.g2d.tilemap

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTiledMap
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.util.viewport.ExtendViewport

/**
 * Load and render a Tiled map, and its objects, with a [SpriteBatch]. Press C to toggle rendering
 * the tiles with a [SpriteCache] instead. Move the camera with WASD.
 *
 * @author Colton Daily
 * @date 4/16/2024
 */
class TiledTileMapExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device

        val map = resourcesVfs["tiled/ortho-tiled-world.tmj"].readTiledMap()

        val batch = SpriteBatch(device, graphics, graphics.preferredFormat, 2000)
        val shapeRenderer = ShapeRenderer(batch)
        val cache = SpriteCache(device, graphics.preferredFormat)
        map.addToCache(cache, 0f, 0f, 1 / 8f)
        val viewport = ExtendViewport(30, 16)
        val camera = viewport.camera
        val bgColor = graphics.surfaceColor(map.backgroundColor ?: Color.DARK_GRAY)
        var useCache = false

        onResize { width, height -> viewport.update(width, height) }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            if (input.isKeyJustPressed(Key.C)) useCache = !useCache
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, bgColor)))
                    )
                camera.update()

                if (useCache) {
                    map.updateCachedAnimationTiles(cache)
                    cache.render(renderPass, camera.viewProjection)
                } else {
                    batch.use(renderPass, camera.viewProjection) {
                        shapeRenderer.updatePixelSize(graphics.width)
                        map.render(
                            it,
                            camera,
                            scale = 1 / 8f,
                            displayObjects = true,
                            shapeRenderer = shapeRenderer,
                        )
                    }
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            cache.release()
            map.release()
        }
    }
}
