package com.littlekt.graphics.g2d.tilemap

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.math.Rect
import com.littlekt.util.calculateViewBounds

/**
 * A generic renderable layer of a [TileMap].
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileLayer {
    protected val viewBounds = Rect()

    /** Render the layer. */
    fun render(batch: Batch, camera: Camera, x: Float, y: Float, scale: Float = 1f) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale)
    }

    /** Render the layer. */
    abstract fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f
    )

    /** Add the layer to a [SpriteCache]. */
    abstract fun addToCache(cache: SpriteCache, x: Float = 0f, y: Float = 0f, scale: Float = 1f)

    /** Remove the layer from a [SpriteCache]. */
    abstract fun removeFromCache(cache: SpriteCache)
}
