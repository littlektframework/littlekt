package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledObjectLayer(
    type: String,
    name: String,
    id: Int,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    val drawOrder: TiledMap.Object.DrawOrder?,
    val objects: List<TiledMap.Object>
) : TiledLayer(type, name, id, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties) {
    val objectsById by lazy { objects.associateBy { it.id } }
    val objectsByName by lazy { objects.associateBy { it.name } }
    val objectsByType by lazy { objects.groupBy { it.type } }

    fun getById(id: Int): TiledMap.Object? = objectsById[id]
    fun getByName(name: String): TiledMap.Object? = objectsByName[name]
    fun getByType(type: String): List<TiledMap.Object> = objects.filter { it.type == type }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
        // TODO add drawing objects on 'debug' mode when drawing primitives is supported
    }
}