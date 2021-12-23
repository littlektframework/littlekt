package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.Rect
import kotlin.math.ceil
import kotlin.math.floor
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
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : LDtkLayer(
    identifier, type, cellSize, gridWidth, gridHeight, pxTotalOffsetX, pxTotalOffsetY, opacity
) {

    val autoTilesCoordIdMap: Map<Int, AutoTile> = autoTiles.associateBy {
        getCoordId(it.renderX / cellSize, it.renderY / cellSize)
    }


    internal fun getAutoLayerLDtkTile(
        autoTile: AutoTile,
    ): LDtkTileset.LDtkTile? {
        if (autoTile.tileId < 0) {
            return null
        }
        return tileset.getLDtkTile(autoTile.tileId, autoTile.flips)
    }

    operator fun get(coordId: Int) = autoTilesCoordIdMap[coordId]

    override fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float) {
        val minY = max(floor(-viewBounds.y / cellSize).toInt(), 0)
        val maxY = min(ceil((-viewBounds.y + viewBounds.height) / cellSize).toInt(), gridHeight)
        val minX = max(floor(viewBounds.x / cellSize).toInt(), 0)
        val maxX = min(ceil((viewBounds.x + viewBounds.width) / cellSize).toInt(), gridWidth)
        autoTiles.forEach { autoTile ->
            val rx = autoTile.renderX + pxTotalOffsetX + x
            val ry = autoTile.renderY + pxTotalOffsetY + y
            // if (rx / cellSize in minX..maxX && ry / cellSize in minY..maxY) {
            getAutoLayerLDtkTile(autoTile)?.also {
                batch.draw(
                    slice = it.slice,
                    x = rx,
                    y = ry,
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
            //}
        }
    }

    data class AutoTile(val tileId: Int, val flips: Int, val renderX: Int, val renderY: Int)
}