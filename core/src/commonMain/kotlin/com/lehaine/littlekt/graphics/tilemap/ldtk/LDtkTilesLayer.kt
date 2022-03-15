package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkTilesLayer(
    val tileset: LDtkTileset,
    val tiles: Map<Int, List<TileInfo>>,
    identifier: String,
    iid: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : LDtkLayer(
    identifier, iid, type, cellSize, gridWidth, gridHeight, pxTotalOffsetX, pxTotalOffsetY, opacity
) {

    fun getTileStackAt(cx: Int, cy: Int): List<TileInfo> {
        return if (isCoordValid(cx, cy)) {
            tiles[getCoordId(cx, cy)] ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun hasAnyTileAt(cx: Int, cy: Int): Boolean {
        return tiles.contains(getCoordId(cx, cy))
    }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val cellSize = cellSize * scale
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale
        val minX = max(0, ((viewBounds.x - x - pxTotalOffsetX) / cellSize).toInt())
        val maxX = min(
            gridWidth,
            ((viewBounds.x + viewBounds.width - x - pxTotalOffsetX) / cellSize).toInt()
        )
        val minY = max(0, ((viewBounds.y - y - pxTotalOffsetY) / cellSize).toInt())
        val maxY = min(
            gridHeight,
            ((viewBounds.y + viewBounds.height - y - pxTotalOffsetY) / cellSize).toInt()
        )

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
                                scaleX = scale,
                                scaleY = scale,
                                rotation = Angle.ZERO,
                                flipX = it.flipX,
                                flipY = it.flipY
                            )
                        }
                    }
                }
            }
        }
    }

    data class TileInfo(val tileId: Int, val flipBits: Int)
}