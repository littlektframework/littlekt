package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.Angle
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
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    private val orientation: TiledMap.Orientation,
    private val tileData: IntArray,
    private val tiles: Map<Int, TiledTileset.Tile>
) : TiledLayer(
    type, name, id, width, height, offsetX, offsetY, tileWidth, tileHeight, tintColor, opacity, properties
) {
    private val lastFrameTimes by lazy { mutableMapOf<Int, Duration>() }
    private val lastFrameIndex by lazy { mutableMapOf<Int, Int>() }
    private val flipData = TileData()

    private val screenPos = MutableVec3f()
    private val topLeft = MutableVec2f()
    private val bottomRight = MutableVec2f()

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
        when (orientation) {
            TiledMap.Orientation.ORTHOGONAL -> renderOrthographically(batch, viewBounds, x, y)
            TiledMap.Orientation.ISOMETRIC -> renderIsometrically(batch, viewBounds, x, y)
            TiledMap.Orientation.STAGGERED -> TODO()
            else -> error("$orientation is not currently supported!")
        }
    }

    private fun renderOrthographically(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
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
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData()
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        batch.draw(
                            slice = slice,
                            x = cx * tileWidth + offsetX + x + it.offsetX,
                            y = cy * tileHeight + offsetY + y + it.offsetY,
                            originX = 0f,
                            originY = 0f,
                            width = it.width.toFloat(),
                            height = it.height.toFloat(),
                            scaleX = 1f,
                            scaleY = 1f,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY
                        )
                    }
                }
            }
        }
    }

    private fun renderIsometrically(batch: Batch, viewBounds: Rect, x: Float, y: Float) {
        topLeft.set(viewBounds.x - x - offsetX, viewBounds.y - y - offsetY)
        bottomRight.set(viewBounds.x + viewBounds.width - x - offsetX, viewBounds.y + viewBounds.height - y - offsetY)

        val minX = max(0, (topLeft.toIso().x / tileWidth).toInt())
        val maxX = min(width, (bottomRight.toIso().x / tileWidth).toInt())
        val minY = max(0, (topLeft.toIso().y / tileHeight).toInt())
        val maxY = min(height, (bottomRight.toIso().y / tileHeight).toInt())

        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData()
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val halfWidth = tileWidth * 0.5f
                        val halfHeight = tileHeight * 0.5f

                        val tx = (cx * halfWidth) - (cy * halfWidth)
                        val ty = (cy * halfHeight) + (cx * halfHeight)

                        batch.draw(
                            slice = slice,
                            x = tx + offsetX + x + it.offsetX,
                            y = ty + offsetY + y + it.offsetY,
                            originX = 0f,
                            originY = 0f,
                            width = it.width.toFloat(),
                            height = it.height.toFloat(),
                            scaleX = 1f,
                            scaleY = 1f,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY
                        )
                    }
                }
            }
        }
    }

    private data class TileData(
        var id: Int = 0,
        var flipX: Boolean = false,
        var flipY: Boolean = false,
        var rotation: Angle = Angle.ZERO
    )

    private fun Int.bitsToTileData(): TileData {
        val bits = this
        val flipHorizontally = (bits and TiledMap.FLAG_FLIP_HORIZONTALLY) != 0
        val flipVertically = (bits and TiledMap.FLAG_FLIP_VERTICALLY) != 0
        val flipDiagonally = (bits and TiledMap.FLAG_FLIP_DIAGONALLY) != 0
        val tileId = bits and TiledMap.MASK_CLEAR.inv()

        var flipX = false
        var flipY = false
        var rotation = Angle.ZERO
        if (flipDiagonally) {
            if (flipHorizontally && flipVertically) {
                flipX = true
                rotation = (-270).degrees
            } else if (flipHorizontally) {
                rotation = (-270).degrees
            } else if (flipVertically) {
                rotation = (-90).degrees
            } else {
                flipY = true
                rotation = (-270).degrees
            }
        } else {
            flipX = flipHorizontally
            flipY = flipVertically
        }
        return flipData.also {
            it.flipX = flipX
            it.flipY = flipY
            it.rotation = rotation
            it.id = tileId
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
                .rotate(0f, 0f, 1f, -45f)
                .invert()
        }
    }
}