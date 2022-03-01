package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledTilesLayer(
    type: String,
    name: String,
    id: Int,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    private val tileData: IntArray,
    private val tiles: Map<Int, TextureSlice>
) : TiledLayer(
    type, name, id, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {
    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
        val minX = max(0, ((viewBounds.x - x - offsetX) / tileWidth).toInt())
        val maxX = min(
            width,
            ((viewBounds.x + viewBounds.width - x - offsetX) / tileWidth).toInt()
        )
        val minY = max(0, ((viewBounds.y - y - offsetY) / tileHeight).toInt())
        val maxY = min(
            height,
            ((viewBounds.y + viewBounds.height - y - offsetY) / tileHeight).toInt()
        )
        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (cid < tileData.size) {
                    val tileId = tileData[cid]
                    tiles[tileId]?.let {
                        batch.draw(
                            slice = it,
                            x = cx * tileWidth + offsetX + x,
                            y = cy * tileHeight + offsetY + y,
                            originX = 0f,
                            originY = 0f,
                            width = tileWidth.toFloat(),
                            height = tileHeight.toFloat(),
                            scaleX = 1f,
                            scaleY = 1f,
                            rotation = Angle.ZERO,
//                        flipX = it.flipX,
//                        flipY = it.flipY
                        )
                    }
                }
            }
        }
    }

    data class TileInfo(val tileId: Int, val flipBits: Int)
}