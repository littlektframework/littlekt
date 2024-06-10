package com.littlekt.graphics.g2d.tilemap

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.math.Rect

/**
 * A generic renderable tilemap.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileMap {
    protected val viewBounds = Rect()

    /** Render the entire tile map using a [Batch]. */
    abstract fun render(
        batch: Batch,
        camera: Camera,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f
    )

    /** Add the entire tile map to a [SpriteCache]. */
    abstract fun addToCache(cache: SpriteCache, x: Float = 0f, y: Float = 0f, scale: Float = 1f)

    /** Remove the tile map from a [SpriteCache]. */
    abstract fun removeFromCache(cache: SpriteCache)
}
