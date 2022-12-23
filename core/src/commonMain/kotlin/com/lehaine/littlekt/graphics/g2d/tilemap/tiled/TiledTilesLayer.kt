package com.lehaine.littlekt.graphics.g2d.tilemap.tiled

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.internal.now
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
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
    visible: Boolean,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    private val staggerIndex: TiledMap.StaggerIndex?,
    private val staggerAxis: TiledMap.StaggerAxis?,
    private val orientation: TiledMap.Orientation,
    private val tileData: IntArray,
    private val tiles: Map<Int, TiledTileset.Tile>
) : TiledLayer(
    type, name, id, visible, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {
    private val lastFrameTimes by lazy { mutableMapOf<Int, Duration>() }
    private val lastFrameIndex by lazy { mutableMapOf<Int, Int>() }
    private val flipData = TileData()

    private val screenPos = MutableVec3f()
    private val topLeft = MutableVec2f()
    private val bottomRight = MutableVec2f()

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float, displayObjects: Boolean) {
        if (!visible) return

        when (orientation) {
            TiledMap.Orientation.ORTHOGONAL -> renderOrthographically(batch, viewBounds, x, y, scale)
            TiledMap.Orientation.ISOMETRIC -> renderIsometrically(batch, viewBounds, x, y, scale)
            TiledMap.Orientation.STAGGERED -> renderStaggered(batch, viewBounds, x, y, scale)
            else -> error("$orientation is not currently supported!")
        }
    }

    private fun renderOrthographically(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        val minX = max(0, ((viewBounds.x - x - offsetX) / tileWidth).toInt())
        val maxX = min(
            width - 1,
            ((viewBounds.x + viewBounds.width - x - offsetX) / tileWidth).toInt()
        )
        val minY = max(0, ((viewBounds.y - y - offsetY) / tileHeight).toInt())
        val maxY = min(
            height - 1,
            ((viewBounds.y + viewBounds.height - y - offsetY) / tileHeight).toInt()
        )
        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        batch.draw(
                            slice = slice,
                            x = cx * tileWidth + offsetX + x + it.offsetX * scale,
                            y = cy * tileHeight + offsetY + y + it.offsetY * scale,
                            originX = 0f,
                            originY = 0f,
                            scaleX = scale,
                            scaleY = scale,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY,
                            colorBits = colorBits
                        )
                    }
                }
            }
        }
    }

    private fun renderIsometrically(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        topLeft.set(viewBounds.x - x - offsetX, viewBounds.y - y - offsetY)
        bottomRight.set(
            viewBounds.x + viewBounds.width - x - offsetX,
            viewBounds.y + viewBounds.height - y - offsetY
        )

        val widthHeightRatio = ((width + height) * 0.5f).toInt()
        val minX = (topLeft.toIso().x / tileWidth).toInt() - widthHeightRatio
        val maxX = (bottomRight.toIso().x / tileWidth).toInt() + widthHeightRatio
        val minY = (topLeft.toIso().y / tileHeight).toInt() - widthHeightRatio * 2
        val maxY = (bottomRight.toIso().y / tileHeight).toInt() + widthHeightRatio


        if (maxX < 0 || maxY < 0) return
        if (minX > width || minY > height) return

        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (isCoordValid(cx, cy) && cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val halfWidth = tileWidth * 0.5f
                        val halfHeight = tileHeight * 0.5f

                        val tx =
                            (cx * halfWidth) - (cy * halfWidth) + (height - 1) * halfWidth + offsetX + x + it.offsetX * scale
                        val ty = (cy * halfHeight) + (cx * halfHeight) + offsetY + y + it.offsetY * scale

                        if (viewBounds.intersects(tx, ty, tx + it.width.toFloat(), ty + it.height.toFloat())) {
                            batch.draw(
                                slice = slice,
                                x = tx,
                                y = ty,
                                originX = 0f,
                                originY = 0f,
                                scaleX = scale,
                                scaleY = scale,
                                rotation = tileData.rotation,
                                flipX = tileData.flipX,
                                flipY = tileData.flipY,
                                colorBits = colorBits
                            )
                        }
                    }
                }
            }
        }
    }

    private fun renderStaggered(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        val halfWidth = tileWidth * 0.5f
        val halfHeight = tileHeight * 0.5f


        val minX = max(0, ((viewBounds.x - x - offsetX - halfWidth) / tileWidth).toInt())
        val maxX = min(
            width - 1,
            ((viewBounds.x + viewBounds.width - x - offsetX + halfWidth) / tileWidth).toInt()
        )
        val minY = max(0, ((viewBounds.y - y - offsetY) / tileHeight).toInt())
        val maxY = min(
            height - 1,
            ((viewBounds.y + viewBounds.height - y - offsetY) / halfHeight).toInt()
        )

        for (cy in minY..maxY) {
            val tileOffsetX = if (cy % 2 == 1) halfWidth else 0f
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        batch.draw(
                            slice = slice,
                            x = cx * tileWidth - tileOffsetX + offsetX + x + it.offsetX * scale,
                            y = cy * halfHeight + offsetY + y + it.offsetY * scale,
                            originX = 0f,
                            originY = 0f,
                            scaleX = scale,
                            scaleY = scale,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY,
                            colorBits = colorBits
                        )
                    }
                }
            }
        }
    }

    private fun TiledTileset.Tile.updateFramesAndGetSlice(): TextureSlice {
        return if (frames.isEmpty()) {
            slice
        } else {
            val now = now().milliseconds
            val lastFrameTime = lastFrameTimes.getOrPut(id) { now }
            val lastFrameIndex = lastFrameIndex[id] ?: 0
            val lastFrame = frames[lastFrameIndex]

            val frame = if (now - lastFrameTime >= frames[lastFrameIndex].duration) {
                val nextIdx = if (lastFrameIndex + 1 < frames.size) lastFrameIndex + 1 else 0
                this@TiledTilesLayer.lastFrameIndex[id] = nextIdx
                this@TiledTilesLayer.lastFrameTimes[id] = now
                frames[nextIdx]
            } else {
                lastFrame
            }
            frame.slice
        }
    }

    private fun MutableVec2f.toIso(): Vec3f {
        screenPos.set(x, y, 0f)
        screenPos.mul(isoTransform)
        return screenPos
    }

    companion object {
        private val isoTransform by lazy {
            Mat4()
                .setToIdentity()
                .scale(sqrt(2f) / 2f, sqrt(2f) / 4f, 1f)
                .rotate(0f, 0f, 1f, (-45).degrees)
                .invert()
        }
    }
}