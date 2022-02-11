package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.tilemap.TileLayer
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
enum class LayerType {
    IntGrid,
    Tiles,
    Entities,
    AutoLayer,
    Unknown
}

open class LDtkLayer(
    val identifier: String,
    val type: LayerType,
    /**
     * Grid size in pixels
     */
    val cellSize: Int,

    /**
     * Grid-based layer width
     */
    val gridWidth: Int,

    /**
     * Grid-based layer height
     */
    val gridHeight: Int,

    /**
     * Pixel-based layer X offset (includes both instance and definition offsets)
     */
    val pxTotalOffsetX: Int,

    /**
     * Pixel-based layer Y offset (includes both instance and definition offsets)
     */
    val pxTotalOffsetY: Int,

    /** Layer opacity (0-1) **/
    val opacity: Float
) : TileLayer() {

    /**
     * @return TRUE if grid-based coordinates are within layer bounds.
     */
    fun isCoordValid(cx: Int, cy: Int): Boolean {
        return cx in 0 until gridWidth && cy >= 0 && cy < gridHeight
    }


    fun getCellX(coordId: Int): Int {
        return coordId - coordId / gridWidth * gridWidth
    }

    fun getCellY(coordId: Int): Int {
        return coordId / gridWidth
    }

    fun getCoordId(cx: Int, cy: Int) = cx + cy * gridWidth

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float) {}

    override fun toString(): String {
        return "LDtkLayer(identifier='$identifier', type=$type, cellSize=$cellSize, gridWidth=$gridWidth, gridHeight=$gridHeight, pxTotalOffsetX=$pxTotalOffsetX, pxTotalOffsetY=$pxTotalOffsetY, opacity=$opacity)"
    }


}