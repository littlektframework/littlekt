package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledGroupLayer(
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
    val layers: List<TiledLayer>,
) : TiledLayer(
    type, name, id, visible, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, displayObjects: Boolean) {
        if (!visible) return

        layers.forEach {
            it.render(batch, viewBounds, x + offsetX, y + offsetY, displayObjects)
        }
    }
}