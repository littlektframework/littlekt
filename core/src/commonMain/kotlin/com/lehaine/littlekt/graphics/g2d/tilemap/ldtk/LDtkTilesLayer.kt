package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle

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

        forEachTileInView(viewBounds, x, y, scale) { cx, cy ->
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

    data class TileInfo(val tileId: Int, val flipBits: Int)
}