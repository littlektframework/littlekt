package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.math.Rect
import com.littlekt.math.geom.Angle

/**
 * An "IntGrid Auto" layer of LDtk.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkIntGridAutoLayer(
    val tileset: LDtkTileset,
    val autoTiles: List<LDtkAutoLayer.AutoTile>,
    intGridValueInfo: List<ValueInfo>,
    intGrid: Map<Int, Int>,
    identifier: String,
    iid: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) :
    LDtkIntGridLayer(
        intGridValueInfo,
        intGrid,
        identifier,
        iid,
        type,
        cellSize,
        gridWidth,
        gridHeight,
        pxTotalOffsetX,
        pxTotalOffsetY,
        opacity
    ) {
    val autoTilesCoordIdMap: Map<Int, LDtkAutoLayer.AutoTile> by lazy {
        autoTiles.associateBy { getCoordId(it.renderX / cellSize, it.renderY / cellSize) }
    }

    operator fun get(coordId: Int) = autoTilesCoordIdMap[coordId]

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val minY = 0
        val minX = 0
        val maxY = gridHeight
        val maxX = gridWidth
        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                val autoTile = autoTilesCoordIdMap[getCoordId(cx, cy)] ?: continue
                getAutoLayerLDtkTile(autoTile)?.also {
                    cacheIds +=
                        cache.add(it.slice) {
                            position.set(
                                autoTile.renderX * scale +
                                    pxTotalOffsetX +
                                    x +
                                    it.slice.width * 0.5f * scale,
                                autoTile.renderY * scale +
                                    pxTotalOffsetY +
                                    y +
                                    it.slice.height * 0.5f * scale
                            )
                            this.scale.set(
                                if (it.flipX) -scale else scale,
                                if (it.flipY) -scale else scale
                            )
                        }
                }
            }
        }
    }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale

        forEachTileInView(viewBounds, x, y, scale) { cx, cy ->
            val autoTile = autoTilesCoordIdMap[getCoordId(cx, cy)] ?: return@forEachTileInView
            getAutoLayerLDtkTile(autoTile)?.also {
                batch.draw(
                    slice = it.slice,
                    x = autoTile.renderX * scale + pxTotalOffsetX + x,
                    y = autoTile.renderY * scale + pxTotalOffsetY + y,
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
