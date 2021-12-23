package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.LayerInstance
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect
import kotlin.math.floor
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkTilesLayer(
    tilesets: Map<Int, LDtkTileset>,
    json: LayerInstance
) : LDtkLayer(json) {
    data class TileInfo(val tileId: Int, val flipBits: Int)

    val tileset = tilesets[json.tilesetDefUid]
        ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}")

    val tiles = hashMapOf<Int, List<TileInfo>>()

    init {
        json.gridTiles.forEach {
            if (!tiles.containsKey(it.d[0])) {
                tiles[it.d[0]] = mutableListOf(TileInfo(it.t, it.f))
            } else {
                val mutList = tiles[it.d[0]] as MutableList
                mutList.add(TileInfo(it.t, it.f))
            }
        }
    }

    fun getTileStackAt(cx: Int, cy: Int): List<TileInfo> {
        return if (isCoordValid(cx, cy) && tiles.contains(getCoordId(cx, cy))) {
            tiles[getCoordId(cx, cy)] ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun hasAnyTileAt(cx: Int, cy: Int): Boolean {
        return tiles.contains(getCoordId(cx, cy))
    }

    override fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float) {
        val minY = max(floor(-viewBounds.y / cellSize).toInt(), 0)
        val maxY = gridHeight //min(ceil((-viewBounds.y + viewBounds.height) / cellSize).toInt(), gridHeight)
        val minX = max(floor(viewBounds.x / cellSize).toInt(), 0)
        val maxX = gridWidth //min(ceil((viewBounds.x + viewBounds.width) / cellSize).toInt(), gridWidth)
        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                if (hasAnyTileAt(cx, cy)) {
                    getTileStackAt(cx, cy).forEach { tileInfo ->
                        tileset.getLDtkTile(
                            tileInfo.tileId, tileInfo.flipBits
                        )?.also {
                            batch.draw(
                                slice = it.slice,
                                x = cx * cellSize + pxTotalOffsetX + x,
                                y = cy * cellSize + pxTotalOffsetY + y,
                                originX = 0f,
                                originY = 0f,
                                width = cellSize.toFloat(),
                                height = cellSize.toFloat(),
                                scaleX = 1f,
                                scaleY = 1f,
                                rotation = 0f,
                                flipX = it.flipX,
                                flipY = it.flipY
                            )
                        }
                    }
                }
            }
        }
    }
}