package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.littlekt.math.*
import com.littlekt.math.geom.degrees
import com.littlekt.util.now
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A Tiled "Tiles" layer.
 *
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
    private val tiles: Map<Int, TiledTileset.Tile>,
) :
    TiledLayer(
        type,
        name,
        id,
        visible,
        width,
        height,
        offsetX,
        offsetY,
        tileWidth,
        tileHeight,
        tintColor,
        opacity,
        properties,
    ) {
    private val lastFrameTimes by lazy { mutableMapOf<Int, Duration>() }
    private val lastFrameIndex by lazy { mutableMapOf<Int, Int>() }
    private val flipData = TileData()
    private val cacheAnimatedTilesIds = mutableMapOf<Int, Int>()

    private val screenPos = MutableVec3f()
    private val topLeft = MutableVec2f()
    private val topRight = MutableVec2f()
    private val bottomRight = MutableVec2f()
    private val bottomLeft = MutableVec2f()

    /** Returns [TileData.id] of a tile with coordinates of [x], [y]. */
    fun getTileId(x: Int, y: Int): Int = tileData[getCoordId(x, y)].bitsToTileId()

    override fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
        displayObjects: Boolean,
        shapeRenderer: ShapeRenderer?,
    ) {
        if (!visible) return

        when (orientation) {
            TiledMap.Orientation.ORTHOGONAL ->
                renderOrthographically(batch, viewBounds, x, y, scale)
            TiledMap.Orientation.ISOMETRIC -> renderIsometrically(batch, viewBounds, x, y, scale)
            TiledMap.Orientation.STAGGERED -> {
                if (staggerAxis == TiledMap.StaggerAxis.X) {
                    renderStaggeredXAxis(batch, viewBounds, x, y, scale)
                } else {
                    renderStaggeredYAxis(batch, viewBounds, x, y, scale)
                }
            }
            else -> error("$orientation is not currently supported!")
        }
    }

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        when (orientation) {
            TiledMap.Orientation.ORTHOGONAL -> addToCacheOrthographically(cache, x, y, scale)
            TiledMap.Orientation.ISOMETRIC -> addToCacheIsometrically(cache, x, y, scale)
            TiledMap.Orientation.STAGGERED -> {
                if (staggerAxis == TiledMap.StaggerAxis.X) {
                    addToCacheStaggeredXAxis(cache, x, y, scale)
                } else {
                    addToCacheStaggeredYAxis(cache, x, y, scale)
                }
            }
            else -> error("$orientation is not currently supported!")
        }
    }

    /** Update any animated tiles in the given [SpriteCache]. */
    override fun updateAnimTiles(cache: SpriteCache) {
        when (orientation) {
            TiledMap.Orientation.ORTHOGONAL -> updateCacheOrthographically(cache)
            TiledMap.Orientation.ISOMETRIC -> updateCacheIsometrically(cache)
            TiledMap.Orientation.STAGGERED -> {
                if (staggerAxis == TiledMap.StaggerAxis.X) {
                    updateCacheStaggeredXAxis(cache)
                } else {
                    updateCacheStaggeredYAxis(cache)
                }
            }
            else -> error("$orientation is not currently supported!")
        }
    }

    private fun updateCacheOrthographically(cache: SpriteCache) {
        cacheAnimatedTilesIds.forEach { (id, tileDataId) ->
            tiles[tileDataId]?.let {
                val slice = it.updateFramesAndGetSlice()
                cache.updateSprite(id, slice) {
                    // do nothing since we only want to update the slice and not anything else
                }
            }
        }
    }

    private fun updateCacheIsometrically(cache: SpriteCache) {
        cacheAnimatedTilesIds.forEach { (id, tileDataId) ->
            tiles[tileDataId]?.let {
                val slice = it.updateFramesAndGetSlice()
                cache.updateSprite(id, slice) {
                    // do nothing since we only want to update the slice and not anything else
                }
            }
        }
    }

    private fun updateCacheStaggeredXAxis(cache: SpriteCache) {
        cacheAnimatedTilesIds.forEach { (id, tileDataId) ->
            tiles[tileDataId]?.let {
                val slice = it.updateFramesAndGetSlice()
                cache.updateSprite(id, slice) {
                    // do nothing since we only want to update the slice and not anything else
                }
            }
        }
    }

    private fun updateCacheStaggeredYAxis(cache: SpriteCache) {
        cacheAnimatedTilesIds.forEach { (id, tileDataId) ->
            tiles[tileDataId]?.let {
                val slice = it.updateFramesAndGetSlice()
                cache.updateSprite(id, slice) {
                    // do nothing since we only want to update the slice and not anything else
                }
            }
        }
    }

    private fun addToCacheOrthographically(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        for (cy in height downTo 0) {
            for (cx in 0 until width) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val spriteId =
                            cache.add(slice) {
                                val tx =
                                    cx * tileWidth +
                                        offsetX +
                                        x +
                                        it.offsetX * scale +
                                        tileWidth * 0.5f * scale
                                val ty =
                                    cy * tileHeight +
                                        offsetY +
                                        y +
                                        it.offsetY * scale +
                                        tileHeight * 0.5f * scale

                                val scaleX = if (tileData.flipX) -scale else scale
                                val scaleY = if (tileData.flipY) -scale else scale
                                position.set(tx, ty)
                                this.scale.set(scaleX, scaleY)
                                rotation = tileData.rotation
                                color.set(tintColor ?: Color.WHITE)
                            }
                        if (it.frames.isNotEmpty()) {
                            cacheAnimatedTilesIds[spriteId] = tileData.id
                        }
                        cacheIds += spriteId
                    }
                }
            }
        }
    }

    private fun addToCacheIsometrically(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        for (cy in height downTo 0) {
            for (cx in 0 until width) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val spriteId =
                            cache.add(slice) {
                                val halfWidth = tileWidth * 0.5f
                                val halfHeight = tileHeight * 0.5f

                                val tx =
                                    (cx * halfWidth) +
                                        (cy * halfWidth) +
                                        offsetX +
                                        x +
                                        it.offsetX * scale
                                val ty =
                                    (cy * halfHeight) - (cx * halfHeight) +
                                        offsetY +
                                        y +
                                        it.offsetY * scale

                                val scaleX = if (tileData.flipX) -scale else scale
                                val scaleY = if (tileData.flipY) -scale else scale
                                position.set(tx, ty)
                                this.scale.set(scaleX, scaleY)
                                rotation = tileData.rotation
                                color.set(tintColor ?: Color.WHITE)
                            }
                        if (it.frames.isNotEmpty()) {
                            cacheAnimatedTilesIds[spriteId] = tileData.id
                        }
                        cacheIds += spriteId
                    }
                }
            }
        }
    }

    private fun addToCacheStaggeredXAxis(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale
        val staggerIndexEven = staggerIndex == TiledMap.StaggerIndex.EVEN
        val minXA = if (staggerIndexEven) 1 else 0
        val minXB = if (staggerIndexEven) 0 else 1
        for (cy in height downTo 0) {
            for (cx in minXA until width step 2) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val spriteId =
                            cache.add(slice) {
                                val halfWidth = tileWidth * 0.5f
                                val halfHeight = tileHeight * 0.5f
                                val tx = cx * halfWidth + offsetX + x + it.offsetX * scale
                                val ty =
                                    (cy * tileHeight) +
                                        halfHeight +
                                        offsetY +
                                        y +
                                        it.offsetY * scale

                                val scaleX = if (tileData.flipX) -scale else scale
                                val scaleY = if (tileData.flipY) -scale else scale
                                position.set(tx, ty)
                                this.scale.set(scaleX, scaleY)
                                rotation = tileData.rotation
                                color.set(tintColor ?: Color.WHITE)
                            }
                        if (it.frames.isNotEmpty()) {
                            cacheAnimatedTilesIds[spriteId] = tileData.id
                        }
                        cacheIds += spriteId
                    }
                }
            }
            for (cx in minXB until width step 2) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val spriteId =
                            cache.add(slice) {
                                val halfWidth = tileWidth * 0.5f
                                val tx = cx * halfWidth + offsetX + x + it.offsetX * scale
                                val ty = cy * tileHeight + offsetY + y + it.offsetY * scale

                                val scaleX = if (tileData.flipX) -scale else scale
                                val scaleY = if (tileData.flipY) -scale else scale
                                position.set(tx, ty)
                                this.scale.set(scaleX, scaleY)
                                rotation = tileData.rotation
                                color.set(tintColor ?: Color.WHITE)
                            }
                        if (it.frames.isNotEmpty()) {
                            cacheAnimatedTilesIds[spriteId] = tileData.id
                        }
                        cacheIds += spriteId
                    }
                }
            }
        }
    }

    private fun addToCacheStaggeredYAxis(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale
        val halfWidth = tileWidth * 0.5f
        val halfHeight = tileHeight * 0.5f
        val staggerIndexValue = if (staggerIndex == TiledMap.StaggerIndex.EVEN) 0 else 1

        for (cy in height downTo 0) {
            val tileOffsetX = if (cy % 2 == staggerIndexValue) halfWidth else 0f
            for (cx in 0 until width) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val spriteId =
                            cache.add(slice) {
                                val tx =
                                    cx * tileWidth - tileOffsetX + offsetX + x + it.offsetX * scale
                                val ty = cy * halfHeight + offsetY + y + it.offsetY * scale

                                val scaleX = if (tileData.flipX) -scale else scale
                                val scaleY = if (tileData.flipY) -scale else scale
                                position.set(tx, ty)
                                this.scale.set(scaleX, scaleY)
                                rotation = tileData.rotation
                                color.set(tintColor ?: Color.WHITE)
                            }
                        if (it.frames.isNotEmpty()) {
                            cacheAnimatedTilesIds[spriteId] = tileData.id
                        }
                        cacheIds += spriteId
                    }
                }
            }
        }
    }

    private fun renderOrthographically(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
    ) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        val minX = max(0, ((viewBounds.x - x - offsetX) / tileWidth).toInt())
        val maxX =
            min(width - 1, ((viewBounds.x + viewBounds.width - x - offsetX) / tileWidth).toInt())
        val minY = max(0, ((viewBounds.y - y - offsetY) / tileHeight).toInt())
        val maxY =
            min(height - 1, ((viewBounds.y + viewBounds.height - y - offsetY) / tileHeight).toInt())
        for (cy in maxY downTo minY) {
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
                            color = tintColor ?: Color.WHITE,
                        )
                    }
                }
            }
        }
    }

    private fun renderIsometrically(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
    ) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        topLeft.set(viewBounds.x - x - offsetX, viewBounds.y - y - offsetY)
        topRight.set(viewBounds.x2 - x - offsetX, viewBounds.y - y - offsetY)
        bottomRight.set(viewBounds.x2 - x - offsetX, viewBounds.y2 - y - offsetY)
        bottomLeft.set(viewBounds.x - x - offsetX, viewBounds.y2 - y - offsetY)

        val minX = (bottomLeft.toIso().x / tileWidth).toInt() - 2
        val maxX = (topRight.toIso().x / tileWidth).toInt() + 2
        val minY = (topLeft.toIso().y / tileWidth).toInt() - 2
        val maxY = (bottomRight.toIso().y / tileWidth).toInt() + 2

        if (maxX < 0 || maxY < 0) return
        if (minX > width || minY > height) return

        for (cy in maxY downTo minY) {
            for (cx in minX..maxX) {
                val cid = getCoordId(cx, cy)
                if (isCoordValid(cx, cy) && cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        val halfWidth = tileWidth * 0.5f
                        val halfHeight = tileHeight * 0.5f

                        val tx =
                            (cx * halfWidth) + (cy * halfWidth) + offsetX + x + it.offsetX * scale
                        val ty =
                            (cy * halfHeight) - (cx * halfHeight) + offsetY + y + it.offsetY * scale

                        if (
                            viewBounds.intersects(
                                tx,
                                ty,
                                tx + it.width.toFloat(),
                                ty + it.height.toFloat(),
                            )
                        ) {
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
                                color = tintColor ?: Color.WHITE,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun renderStaggeredXAxis(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
    ) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        val halfWidth = tileWidth * 0.5f
        val halfHeight = tileHeight * 0.5f

        val minX = max(0, ((viewBounds.x - x - halfWidth - offsetY) / halfWidth).toInt())
        val maxX = min(width - 1, ((viewBounds.x2 - x + halfWidth - offsetY) / halfWidth).toInt())
        val minY = max(0, ((viewBounds.y - halfHeight - y - offsetX) / tileHeight).toInt() - 2)
        val maxY =
            min(height - 1, ((viewBounds.y2 + tileHeight - y - offsetX) / tileHeight).toInt())
        val staggerIndexEven = staggerIndex == TiledMap.StaggerIndex.EVEN
        val minXA = if (staggerIndexEven == (minX % 2 == 0)) minX + 1 else minX
        val minXB = if (staggerIndexEven == (minX % 2 == 0)) minX else minX + 1

        for (cy in maxY downTo minY) {
            for (cx in minXA..maxX step 2) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        batch.draw(
                            slice = slice,
                            x = cx * halfWidth + offsetX + x + it.offsetX * scale,
                            y = (cy * tileHeight) + halfHeight + offsetY + y + it.offsetY * scale,
                            originX = 0f,
                            originY = 0f,
                            scaleX = scale,
                            scaleY = scale,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY,
                            color = tintColor ?: Color.WHITE,
                        )
                    }
                }
            }
            for (cx in minXB..maxX step 2) {
                val cid = getCoordId(cx, cy)
                if (cid in tileData.indices) {
                    val tileData = tileData[cid].bitsToTileData(flipData)
                    tiles[tileData.id]?.let {
                        val slice = it.updateFramesAndGetSlice()
                        batch.draw(
                            slice = slice,
                            x = cx * halfWidth + offsetX + x + it.offsetX * scale,
                            y = cy * tileHeight + offsetY + y + it.offsetY * scale,
                            originX = 0f,
                            originY = 0f,
                            scaleX = scale,
                            scaleY = scale,
                            rotation = tileData.rotation,
                            flipX = tileData.flipX,
                            flipY = tileData.flipY,
                            color = tintColor ?: Color.WHITE,
                        )
                    }
                }
            }
        }
    }

    private fun renderStaggeredYAxis(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
    ) {
        val tileWidth = tileWidth * scale
        val tileHeight = tileHeight * scale
        val offsetX = offsetX * scale
        val offsetY = offsetY * scale

        val halfWidth = tileWidth * 0.5f
        val halfHeight = tileHeight * 0.5f

        val minX = max(0, ((viewBounds.x - x - offsetX - halfWidth) / tileWidth).toInt())
        val maxX = min(width - 1, ((viewBounds.x2 - x - offsetX + halfWidth) / tileWidth).toInt())
        val minY = max(0, ((viewBounds.y - y - offsetY) / halfHeight).toInt() - 2)
        val maxY = min(height - 1, ((viewBounds.y2 - y - offsetY) / halfHeight).toInt())

        val staggerIndexValue = if (staggerIndex == TiledMap.StaggerIndex.EVEN) 0 else 1
        for (cy in maxY downTo minY) {
            val tileOffsetX = if (cy % 2 == staggerIndexValue) halfWidth else 0f
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
                            color = tintColor ?: Color.WHITE,
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

            val frame =
                if (now - lastFrameTime >= frames[lastFrameIndex].duration) {
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
        screenPos.mul(invIsoTransform)
        return screenPos
    }

    companion object {
        private val invIsoTransform by lazy {
            Mat4()
                .setToIdentity()
                .scale(sqrt(2f) / 2f, sqrt(2f) / 4f, 1f)
                .rotate(0f, 0f, 1f, (-45).degrees)
                .invert()
        }
    }
}
