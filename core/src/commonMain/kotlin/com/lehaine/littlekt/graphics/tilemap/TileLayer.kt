package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileLayer {
    protected val viewBounds = Rect()

    fun render(batch: Batch, camera: Camera, x: Float, y: Float) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y)
    }

    abstract fun render(batch: Batch, viewBounds: Rect, x: Float = 0f, y: Float = 0f)
}