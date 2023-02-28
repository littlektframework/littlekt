package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.tilemap.TileLayer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import kotlin.math.max
import kotlin.math.min

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

    val iid: String,

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
    val opacity: Float,
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

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) {}

    /**
     * Iterate through the tiles in view.
     */
    fun forEachTileInView(
        camera: Camera,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        action: (cx: Int, cy: Int) -> Unit,
    ) {
        viewBounds.calculateViewBounds(camera)
        forEachTileInView(viewBounds, x, y, scale, action)
    }

    /**
     * Iterate through the tiles in view.
     */
    inline fun forEachTileInView(
        viewBounds: Rect,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        action: (cx: Int, cy: Int) -> Unit,
    ) {
        val cellSize = cellSize * scale
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale
        val minX = max(0, ((viewBounds.x - x - pxTotalOffsetX) / cellSize).toInt())
        val maxX = min(
            gridWidth,
            ((viewBounds.x2 - x - pxTotalOffsetX) / cellSize).toInt()
        )
        val minY = max(0, ((viewBounds.y - y - pxTotalOffsetY) / cellSize).toInt())
        val maxY = min(
            gridHeight,
            ((viewBounds.y2 - y - pxTotalOffsetY) / cellSize).toInt()
        )

        for (cy in minY..maxY) {
            for (cx in minX..maxX) {
                action(cx, cy)
            }
        }
    }

    override fun toString(): String {
        return "LDtkLayer(identifier='$identifier', type=$type, cellSize=$cellSize, gridWidth=$gridWidth, gridHeight=$gridHeight, pxTotalOffsetX=$pxTotalOffsetX, pxTotalOffsetY=$pxTotalOffsetY, opacity=$opacity)"
    }


}