package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.internal.now
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    private val tiles: Map<Int, TiledTileset.Tile>
) : TiledLayer(
    type, name, id, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {
    private val lastFrameTimes by lazy { mutableMapOf<Int, Duration>() }
    private val lastFrameIndex by lazy { mutableMapOf<Int, Int>() }

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
                        if (it.frames.isEmpty()) {
                            batch.draw(
                                slice = it.slice,
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
                        } else {
                            val now = now().milliseconds
                            val lastFrameTime = lastFrameTimes.getOrPut(it.id) { now }
                            val lastFrameIndex = lastFrameIndex[it.id] ?: 0
                            val lastFrame = it.frames[lastFrameIndex]

                            val frame = if (now - lastFrameTime >= it.frames[lastFrameIndex].duration) {
                                val nextIdx = if (lastFrameIndex + 1 < it.frames.size) lastFrameIndex + 1 else 0
                                this.lastFrameIndex[it.id] = nextIdx
                                this.lastFrameTimes[it.id] = now
                                it.frames[nextIdx]
                            } else {
                                lastFrame
                            }

                            batch.draw(
                                slice = frame.slice,
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
}