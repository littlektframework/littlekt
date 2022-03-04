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
    val autoTilesCoordIdMap: Map<Int, AutoTile> = autoTiles.associateBy {
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

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
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
        autoTiles.forEach { autoTile ->
            val rx = autoTile.renderX
            val ry = autoTile.renderY
            val tx = rx / cellSize
            val ty = ry / cellSize
            if (tx in minX..maxX && ty in minY..maxY) {
                getAutoLayerLDtkTile(autoTile)?.also {
                    batch.draw(
                        slice = it.slice,
                        x = rx + pxTotalOffsetX + x,
                        y = ry + pxTotalOffsetY + y,
                        originX = 0f,
                        originY = 0f,
                        width = cellSize.toFloat(),
                        height = cellSize.toFloat(),
                        scaleX = 1f,
                        scaleY = 1f,
                        rotation = Angle.ZERO,
                        flipX = it.flipX,
                        flipY = it.flipY
                    )
                }
            }
        }
    }

    data class AutoTile(val tileId: Int, val flips: Int, val renderX: Int, val renderY: Int)
}