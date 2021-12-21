package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Viewport
import com.lehaine.littlekt.math.Rect
import kotlin.math.abs

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileLayer {
    protected val viewBounds = Rect()

    fun render(batch: SpriteBatch, camera: Camera, viewport: Viewport, x: Float, y: Float) {
        viewBounds.calculateViewBounds(camera, viewport)
        render(batch, viewBounds, x, y)
    }

    abstract fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float)

    private fun Rect.calculateViewBounds(camera: Camera, viewport: Viewport) {
        val width = viewport.width * camera.zoom
        val height = viewport.height * camera.zoom
        val w = width * abs(camera.up.y) + height * abs(camera.up.x)
        val h = height * abs(camera.up.y) + width * abs(camera.up.x)
        this.x = camera.position.x - w / 2
        this.y = camera.position.y - h / 2
        this.width = w
        this.height = h
    }
}