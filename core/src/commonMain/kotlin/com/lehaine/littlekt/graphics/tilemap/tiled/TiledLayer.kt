package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.tilemap.TileLayer
import com.lehaine.littlekt.graphics.tilemap.tiled.internal.TileData
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
abstract class TiledLayer(
    val type: String,
    val name: String,
    val id: Int,
    val visible: Boolean,
    val width: Int,
    val height: Int,
    val offsetX: Float,
    val offsetY: Float,
    val tileWidth: Int,
    val tileHeight: Int,
    val tintColor: Color?,
    val opacity: Float,
    val properties: Map<String, TiledMap.Property>
) : TileLayer() {

    protected val colorBits = (tintColor?.toMutableColor() ?: Color.WHITE.toMutableColor()).apply {
        a *= opacity
    }.toFloatBits()

    fun render(batch: Batch, camera: Camera, x: Float, y: Float, scale: Float = 1f, displayObjects: Boolean = false) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale, displayObjects = displayObjects)
    }

    final override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) =
        render(batch, viewBounds, x, y, scale, displayObjects = false)

    abstract fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        displayObjects: Boolean = false
    )

    /**
     * @return true if grid-based coordinates are within layer bounds.
     */
    fun isCoordValid(cx: Int, cy: Int): Boolean {
        return cx in 0 until width && cy >= 0 && cy < height
    }

    fun getCellX(coordId: Int): Int {
        return coordId - coordId / width * width
    }

    fun getCellY(coordId: Int): Int {
        return coordId / width
    }

    fun getCoordId(cx: Int, cy: Int) = cx + cy * width

    internal fun Int.bitsToTileData(result: TileData): TileData {
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
        return result.also {
            it.flipX = flipX
            it.flipY = flipY
            it.rotation = rotation
            it.id = tileId
        }
    }
}