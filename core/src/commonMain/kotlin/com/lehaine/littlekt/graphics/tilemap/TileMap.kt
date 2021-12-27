package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import kotlin.math.abs

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
abstract class TileMap {
    protected val viewBounds = Rect()

    abstract fun render(batch: SpriteBatch, camera: Camera, x: Float = 0f, y: Float = 0f)
}