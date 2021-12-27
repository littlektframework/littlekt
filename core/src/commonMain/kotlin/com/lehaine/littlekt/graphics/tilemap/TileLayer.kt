package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileLayer {
    protected val viewBounds = Rect()

    fun render(batch: SpriteBatch, camera: Camera, x: Float, y: Float) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y)
    }

    abstract fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float)
}