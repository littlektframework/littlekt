package com.lehaine.littlekt.graphics.tilemap

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Viewport

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
interface TileMap {

    fun render(batch: SpriteBatch, camera: Camera, viewport: Viewport)
}