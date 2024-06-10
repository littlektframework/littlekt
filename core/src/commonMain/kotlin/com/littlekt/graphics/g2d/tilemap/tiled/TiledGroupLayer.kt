package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.Rect

/**
 * A Tiled "Group" Layer.
 *
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
) :
    TiledLayer(
        type,
        name,
        id,
        visible,
        width,
        height,
        offsetX,
        offsetY,
        tileWidth,
        tileHeight,
        tintColor,
        opacity,
        properties
    ) {

    override fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
        displayObjects: Boolean,
        shapeRenderer: ShapeRenderer?
    ) {
        if (!visible) return

        layers.forEach {
            it.render(
                batch,
                viewBounds,
                x + offsetX * scale,
                y + offsetY * scale,
                scale = scale,
                displayObjects = displayObjects,
                shapeRenderer = shapeRenderer
            )
        }
    }

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        layers.forEach { it.addToCache(cache, x, y, scale) }
    }

    override fun removeFromCache(cache: SpriteCache) {
        layers.forEach { it.removeFromCache(cache) }
    }
}
