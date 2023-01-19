package com.lehaine.littlekt.graphics.g2d.tilemap

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileLayer {
    protected val viewBounds = Rect()

    fun render(batch: Batch, camera: Camera, x: Float, y: Float, scale: Float = 1f) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale)
    }

    abstract fun render(batch: Batch, viewBounds: Rect, x: Float = 0f, y: Float = 0f, scale: Float = 1f)
}