package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.radians
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkIntGridAutoLayer(
    val tileset: LDtkTileset,
    val autoTiles: List<LDtkAutoLayer.AutoTile>,
    intGridValueInfo: List<ValueInfo>,
    intGrid: Map<Int, Int>,
    identifier: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : LDtkIntGridLayer(
    intGridValueInfo,
    intGrid,
    identifier,
    type,
    cellSize,
    gridWidth,
    gridHeight,
    pxTotalOffsetX,
    pxTotalOffsetY,
    opacity
) {

    val autoTilesCoordIdMap: Map<Int, LDtkAutoLayer.AutoTile> = autoTiles.associateBy {
        getCoordId(it.renderX / cellSize, it.renderY / cellSize)
    }

    operator fun get(coordId: Int) = autoTilesCoordIdMap[coordId]

    override fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float) {
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
                        rotation = 0.radians,
                        flipX = it.flipX,
                        flipY = it.flipY
                    )
                }
            }
        }
    }

    private fun getAutoLayerLDtkTile(
        autoTile: LDtkAutoLayer.AutoTile,
    ): LDtkTileset.LDtkTile? {
        if (autoTile.tileId < 0) {
            return null
        }
        return tileset.getLDtkTile(autoTile.tileId, autoTile.flips)
    }

    override fun toString(): String {
        return "LDtkIntGridAutoLayer(autoTiles=$autoTiles, autoTilesCoordIdMap=$autoTilesCoordIdMap, autoTilesCoordIdMap=$autoTilesCoordIdMap)"
    }
}