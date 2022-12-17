package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkAutoLayer(
    val tileset: com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkTileset,
    val autoTiles: List<com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkAutoLayer.AutoTile>,
    identifier: String,
    iid: String,
    type: com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkLayer(
    identifier, iid, type, cellSize, gridWidth, gridHeight, pxTotalOffsetX, pxTotalOffsetY, opacity
) {
    val autoTilesCoordIdMap: Map<Int, com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkAutoLayer.AutoTile> = autoTiles.associateBy {
        getCoordId(it.renderX / cellSize, it.renderY / cellSize)
    }

    private fun getAutoLayerLDtkTile(
        autoTile: com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkAutoLayer.AutoTile,
    ): com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkTileset.LDtkTile? {
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
                val autoTile = autoTilesCoordIdMap[getCoordId(cx, cy)] ?: continue
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
    }

    data class AutoTile(val tileId: Int, val flips: Int, val renderX: Int, val renderY: Int)
}