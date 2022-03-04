package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.tilemap.tiled.internal.TileData
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledObjectLayer(
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
    val drawOrder: TiledMap.Object.DrawOrder?,
    val objects: List<TiledMap.Object>,
    private val tiles: Map<Int, TiledTileset.Tile>
) : TiledLayer(
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

    private val flipData = TileData()

    val objectsById by lazy { objects.associateBy { it.id } }
    val objectsByName by lazy { objects.associateBy { it.name } }
    val objectsByType by lazy { objects.groupBy { it.type } }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, displayObjects: Boolean) {
        if (!displayObjects || !visible) return

        objects.forEach { obj ->
            if (!obj.visible) return@forEach

            obj.gid?.let { gid ->
                val tileData = gid.toInt().bitsToTileData(flipData)
                tiles[tileData.id]?.let {
                    batch.draw(
                        slice = it.slice,
                        x = obj.x + offsetX + x + it.offsetX,
                        y = obj.y + offsetY + y + it.offsetY,
                        originX = 0f,
                        originY = 0f,
                        width = obj.bounds.width,
                        height = obj.bounds.height,
                        scaleX = 1f,
                        scaleY = 1f,
                        rotation = obj.rotation,
                        flipX = tileData.flipX,
                        flipY = tileData.flipY,
                        colorBits = colorBits
                    )
                }
            }
        }
    }

    fun getById(id: Int): TiledMap.Object = objectsById[id] ?: error("Object: '$id' does not exist in this layer!")
    fun getByName(name: String): TiledMap.Object = objectsByName[name] ?: error("Object: '$name' does not exist in this layer!")
    fun getByType(type: String): List<TiledMap.Object> = objects.filter { it.type == type }

    operator fun get(name: String) = getByName(name)
    operator fun get(id: Int) = getById(id)
}