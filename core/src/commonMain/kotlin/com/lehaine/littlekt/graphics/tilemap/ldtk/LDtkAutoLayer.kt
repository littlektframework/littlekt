package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.LayerInstance
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
    tilesets: Map<Int, LDtkTileset>,
    json: LayerInstance
) : LDtkLayer(json) {
    val tileset = tilesets[json.tilesetDefUid]
        ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}")

    val autoTiles = json.autoLayerTiles.map {
        AutoTile(it.t, it.f, it.px[0], it.px[1])
    }

    val autoTilesCoordIdMap = mutableMapOf<Int, AutoTile>()

    init {
        json.autoLayerTiles.forEach {
            val autoTile = AutoTile(
                tileId = it.t,
                flips = it.f,
                renderX = it.px[0],
                renderY = it.px[1]
            )
            autoTilesCoordIdMap[getCoordId(autoTile.renderX / cellSize, autoTile.renderY / cellSize)] = autoTile
        }
    }

    data class AutoTile(val tileId: Int, val flips: Int, val renderX: Int, val renderY: Int)

    internal fun getAutoLayerLDtkTile(
        autoTile: LDtkAutoLayer.AutoTile,
    ): LDtkTileset.LDtkTile? {
        if (autoTile.tileId < 0) {
            return null
        }
        return tileset.getLDtkTile(autoTile.tileId, autoTile.flips)
    }

    override fun render(batch: SpriteBatch, viewBounds: Rect, offsetX: Int, offsetY: Int) {
        val minY = max(floor(-viewBounds.y / cellSize).toInt(), 0)
        val maxY = min(ceil((-viewBounds.y + viewBounds.height) / cellSize).toInt(), gridHeight)
        val minX = max(floor(viewBounds.x / cellSize).toInt(), 0)
        val maxX = min(ceil((viewBounds.x + viewBounds.width) / cellSize).toInt(), gridWidth)
        autoTiles.forEach { autoTile ->
            val rx = (autoTile.renderX + pxTotalOffsetX + offsetX)
            val ry = -(autoTile.renderY + pxTotalOffsetY - gridHeight * cellSize) + offsetY - cellSize
           // if (rx / cellSize in minX..maxX && ry / cellSize in minY..maxY) {
                getAutoLayerLDtkTile(autoTile)?.also {
                    batch.draw(
                        slice = it.slice,
                        x = rx.toFloat(),
                        y = ry.toFloat(), // LDtk is y-down, so invert it
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
}