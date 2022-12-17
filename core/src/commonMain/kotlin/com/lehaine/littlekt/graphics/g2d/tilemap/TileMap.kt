package com.lehaine.littlekt.graphics.g2d.tilemap

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileMap {
    protected val viewBounds = Rect()

    abstract fun render(batch: Batch, camera: Camera, x: Float = 0f, y: Float = 0f, scale: Float = 1f)
}