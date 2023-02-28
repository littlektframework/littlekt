package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkAutoLayer(
    val tileset: LDtkTileset,
    val autoTiles: List<AutoTile>,
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
    val autoTilesCoordIdMap: Map<Int, AutoTile> =
        autoTiles.associateBy {
            getCoordId(it.renderX / cellSize, it.renderY / cellSize)
        }

    private fun getAutoLayerLDtkTile(
        autoTile: AutoTile,
    ): LDtkTileset.LDtkTile? {
        if (autoTile.tileId < 0) {
            return null
        }
        return tileset.getLDtkTile(autoTile.tileId, autoTile.flips)
    }

    operator fun get(coordId: Int) = autoTilesCoordIdMap[coordId]

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val cellSize = cellSize * scale
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale

        forEachTileInView(viewBounds, x, y, scale) { cx, cy ->
            val autoTile = autoTilesCoordIdMap[getCoordId(cx, cy)] ?: return@forEachTileInView
            getAutoLayerLDtkTile(autoTile)?.also {
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

    data class AutoTile(val tileId: Int, val flips: Int, val renderX: Int, val renderY: Int)
}