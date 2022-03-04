package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 3/1/2022
 */
class TiledImageLayer(
    type: String,
    name: String,
    id: Int,
    visible: Boolean,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    private val texture: TextureSlice?
) : TiledLayer(
    type, name, id, visible, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, displayObjects: Boolean) {
        if (!visible) return

        texture?.let {
            val tx = x + offsetX
            val ty = y + offsetY
            val tx2 = tx + texture.width
            val ty2 = ty + texture.height
            if (viewBounds.intersects(tx, ty, tx2, ty2)) {
                batch.draw(it, tx, ty, colorBits = colorBits)
            }
        }
    }
}