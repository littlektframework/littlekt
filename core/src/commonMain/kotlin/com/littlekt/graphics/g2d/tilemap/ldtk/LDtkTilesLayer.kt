package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.math.Rect
import com.littlekt.math.geom.Angle

/**
 * A "Tiles" layer in LDtk.
 *
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
) :
    LDtkLayer(
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

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val minY = 0
        val minX = 0
        val maxY = gridHeight
        val maxX = gridWidth
        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                if (hasAnyTileAt(cx, cy)) {
                    getTileStackAt(cx, cy).forEach { tileInfo ->
                        tileset.getLDtkTile(tileInfo.tileId, tileInfo.flipBits)?.also {
                            cacheIds +=
                                cache.add(it.slice) {
                                    position.set(
                                        tileInfo.renderX * scale +
                                            pxTotalOffsetX +
                                            x +
                                            it.slice.width * 0.5f * scale,
                                        tileInfo.renderY * scale +
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
        }
    }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale

        forEachTileInView(viewBounds, x, y, scale) { cx, cy ->
            if (hasAnyTileAt(cx, cy)) {
                getTileStackAt(cx, cy).forEach { tileInfo ->
                    tileset.getLDtkTile(tileInfo.tileId, tileInfo.flipBits)?.also {
                        batch.draw(
                            slice = it.slice,
                            x = tileInfo.renderX * scale + pxTotalOffsetX + x,
                            y = tileInfo.renderY * scale + pxTotalOffsetY + y,
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

    data class TileInfo(val tileId: Int, val flipBits: Int, val renderX: Int, val renderY: Int)
}
