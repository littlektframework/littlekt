package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
interface TileLayer {

    fun render(batch: SpriteBatch, viewBounds: Rect)
}