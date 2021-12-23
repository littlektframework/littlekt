package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.IntGridValueDefinition
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
open class LDtkIntGridAutoLayer(
    tilesets: Map<Int, LDtkTileset>,
    intGridValues: List<IntGridValueDefinition>,
    json: LayerInstance
) : LDtkIntGridLayer(intGridValues, json) {
    val tileset = tilesets[json.tilesetDefUid]
        ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}")

    val autoTiles: List<LDtkAutoLayer.AutoTile> =
        json.autoLayerTiles.map {
            LDtkAutoLayer.AutoTile(
                tileId = it.t,
                flips = it.f,
                renderX = it.px[0],
                renderY = it.px[1]
            )
        }
    private val _autoTilesCoordIdMap = mutableMapOf<Int, LDtkAutoLayer.AutoTile>()
    private val autoTilesCoordIdMap: Map<Int, LDtkAutoLayer.AutoTile>

    init {
        json.autoLayerTiles.forEach {
            val autoTile = LDtkAutoLayer.AutoTile(
                tileId = it.t,
                flips = it.f,
                renderX = it.px[0],
                renderY = it.px[1]
            )
            _autoTilesCoordIdMap[getCoordId(autoTile.renderX / cellSize, autoTile.renderY / cellSize)] = autoTile
        }
        autoTilesCoordIdMap = _autoTilesCoordIdMap.toMap()
    }

    private fun getAutoLayerLDtkTile(
        autoTile: LDtkAutoLayer.AutoTile,
    ): LDtkTileset.LDtkTile? {
        if (autoTile.tileId < 0) {
            return null
        }
        return tileset.getLDtkTile(autoTile.tileId, autoTile.flips)
    }

    override fun render(batch: SpriteBatch, viewBounds: Rect, x: Float, y: Float) {
        val minY = max(floor(-viewBounds.y / cellSize).toInt(), 0)
        val maxY = min(ceil((-viewBounds.y + viewBounds.height) / cellSize).toInt(), gridHeight)
        val minX = max(floor(viewBounds.x / cellSize).toInt(), 0)
        val maxX = min(ceil((viewBounds.x + viewBounds.width) / cellSize).toInt(), gridWidth)
        autoTiles.forEach { autoTile ->
            val rx = autoTile.renderX + pxTotalOffsetX + x
            val ry = autoTile.renderY + pxTotalOffsetY + y
            //      if (rx / cellSize in minX..maxX && ry / cellSize in minY..maxY) {
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
                //}
            }
        }
    }

    override fun toString(): String {
        return "LDtkIntGridAutoLayer(autoTiles=$autoTiles, _autoTilesCoordIdMap=$_autoTilesCoordIdMap, autoTilesCoordIdMap=$autoTilesCoordIdMap)"
    }
}