package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.SpriteId
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.tilemap.TileLayer
import com.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.littlekt.math.Rect
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.util.calculateViewBounds
import com.littlekt.util.datastructure.fastForEach

/**
 * A base Tiled layer.
 *
 * @author Colton Daily
 * @date 2/28/2022
 */
abstract class TiledLayer(
    /** The Tiled layer type. */
    val type: String,
    /** The name of this layer. */
    val name: String,
    /** This layers identifier. */
    val id: Int,
    /** If this layer is visible or hidden. */
    val visible: Boolean,
    /** The width of this layer in cells. */
    val width: Int,
    /** The height of this layer in cells. */
    val height: Int,
    /** The pixel offset in the x-axis. */
    val offsetX: Float,
    /** The pixel offset in the y-axis. */
    val offsetY: Float,
    /** The width of a single tile cell. */
    val tileWidth: Int,
    /** The height of a single tile cell. */
    val tileHeight: Int,
    /** The tint color of the layer. */
    val tintColor: Color?,
    /** The opacity of the layer. */
    val opacity: Float,
    /**
     * A map of properties. Access by using property name string value.
     *
     * @see [TiledMap.Property]
     */
    val properties: Map<String, TiledMap.Property>
) : TileLayer() {

    protected val cacheIds by lazy { mutableListOf<SpriteId>() }

    /** Render this layer with a camera. */
    fun render(
        batch: Batch,
        camera: Camera,
        x: Float,
        y: Float,
        scale: Float = 1f,
        displayObjects: Boolean = false,
        shapeRenderer: ShapeRenderer? = null
    ) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale, displayObjects = displayObjects, shapeRenderer)
    }

    final override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) =
        render(batch, viewBounds, x, y, scale, displayObjects = false)

    /** Render this layer with within the given viewbounds. */
    abstract fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        displayObjects: Boolean = false,
        shapeRenderer: ShapeRenderer? = null
    )

    override fun removeFromCache(cache: SpriteCache) {
        cacheIds.fastForEach(cache::remove)
        cacheIds.clear()
    }

    /**
     * Update tiles that needed to be animated in the given [SpriteCache]. They must have been added
     * with [addToCache] before this.
     */
    open fun updateAnimTiles(cache: SpriteCache) = Unit

    /** @return true if grid-based coordinates are within layer bounds. */
    fun isCoordValid(cx: Int, cy: Int): Boolean {
        return cx in 0 until width && cy >= 0 && cy < height
    }

    /** Calculate the x-cell from a 1D coord. */
    fun getCellX(coordId: Int): Int {
        return coordId - coordId / width * width
    }

    /** Calculate the y-cell from a 1D coord. */
    fun getCellY(coordId: Int): Int {
        return coordId / width
    }

    /**
     * Calculate the coord ID (1D) of the given x,y cell. Assume `0,0` is bottom left and
     * `width,height` is top right. This will perform the flip to match Tiled's coordinate system.
     */
    fun getCoordId(cx: Int, cy: Int) = cx + (height - 1 - cy) * width

    internal fun Int.bitsToTileId(): Int =
        this and TiledMap.MASK_CLEAR.inv()

    internal fun Int.bitsToTileData(result: TileData): TileData {
        val bits = this
        val flipHorizontally = (bits and TiledMap.FLAG_FLIP_HORIZONTALLY) != 0
        val flipVertically = (bits and TiledMap.FLAG_FLIP_VERTICALLY) != 0
        val flipDiagonally = (bits and TiledMap.FLAG_FLIP_DIAGONALLY) != 0
        val tileId = bits.bitsToTileId()

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
        return result.also {
            it.flipX = flipX
            it.flipY = flipY
            it.rotation = rotation
            it.id = tileId
        }
    }
}
