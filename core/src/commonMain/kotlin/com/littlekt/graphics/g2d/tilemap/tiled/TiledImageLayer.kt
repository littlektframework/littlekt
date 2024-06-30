package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.Rect

/**
 * A Tiled "Image" layer.
 *
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

        texture?.let {
            val tx = x + offsetX * scale
            val ty = y + offsetY * scale + height * tileHeight * scale - texture.height * scale
            val tx2 = tx + texture.width * scale
            val ty2 = ty + texture.height * scale
            if (viewBounds.intersects(tx, ty, tx2, ty2)) {
                batch.draw(
                    it,
                    tx,
                    ty,
                    scaleX = scale,
                    scaleY = scale,
                    color = tintColor ?: Color.WHITE
                )
            }
        }
    }

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        texture?.let {
            val tx = x + offsetX * scale + texture.width * 0.5f * scale
            val ty =
                y + offsetY * scale + height * tileHeight * scale - texture.height * scale +
                    texture.height * 0.5f * scale
            cacheIds +=
                cache.add(it) {
                    position.set(tx, ty)
                    this.scale.set(scale, scale)
                    color.set(tintColor ?: Color.WHITE)
                }
        }
    }
}
