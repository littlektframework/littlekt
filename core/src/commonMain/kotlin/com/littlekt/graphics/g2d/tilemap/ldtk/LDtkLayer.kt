package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.SpriteId
import com.littlekt.graphics.g2d.tilemap.TileLayer
import com.littlekt.math.Rect
import com.littlekt.util.calculateViewBounds
import kotlin.math.max
import kotlin.math.min

/**
 * The type of LDtk layer.
 *
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

/** A layer in LDtk. */
open class LDtkLayer(
    /** The layer identifier. */
    val identifier: String,
    /** The identifier id of the layer. */
    val iid: String,
    /** The layer type. */
    val type: LayerType,

    /** Grid size in pixels */
    val cellSize: Int,

    /** Grid-based layer width; i.e. number of cells wide */
    val gridWidth: Int,

    /** Grid-based layer height; i.e. number of cell high */
    val gridHeight: Int,

    /** Pixel-based layer X offset (includes both instance and definition offsets) */
    val pxTotalOffsetX: Int,

    /** Pixel-based layer Y offset (includes both instance and definition offsets) */
    val pxTotalOffsetY: Int,

    /** Layer opacity (0-1) * */
    val opacity: Float,
) : TileLayer() {
    lateinit var level: LDtkLevel
        internal set

    protected val cacheIds by lazy { mutableListOf<SpriteId>() }

    /** @return TRUE if grid-based coordinates are within layer bounds. */
    fun isCoordValid(cx: Int, cy: Int): Boolean {
        return cx in 0 until gridWidth && cy >= 0 && cy < gridHeight
    }

    /** Calculate the x-cell from a 1D coord. */
    fun getCellX(coordId: Int): Int {
        return coordId - coordId / gridWidth * gridWidth
    }

    /** Calculate the y-cell from a 1D coord. */
    fun getCellY(coordId: Int): Int {
        return coordId / gridWidth
    }

    /**
     * Calculate the coord ID (1D) of the given x,y cell. Assume `0,0` is bottom left and
     * `gridWidth,gridHeight` is top right. This will perform the flip to match LDtk's coordinate
     * system.
     */
    fun getCoordId(cx: Int, cy: Int) = cx + (gridHeight - 1 - cy) * gridWidth

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) = Unit

    override fun removeFromCache(cache: SpriteCache) {
        cacheIds.forEach(cache::remove)
        cacheIds.clear()
    }

    override fun render(batch: Batch, viewBounds: Rect, x: Float, y: Float, scale: Float) = Unit

    /** Iterate through the tiles in view. */
    fun forEachTileInView(
        camera: Camera,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        paddingX: Int = 0,
        paddingY: Int = 0,
        action: (cx: Int, cy: Int) -> Unit,
    ) {
        viewBounds.calculateViewBounds(camera)
        forEachTileInView(viewBounds, x, y, scale, paddingX, paddingY, action)
    }

    /**
     * Iterate through the tiles in view. `0,0` is bottom-left and `gridWidth,gridHeight` is
     * top-right.
     *
     * @param viewBounds the viewBounds rectangle
     * @param x the x position of the layer
     * @param y the y position of the layer
     * @param scale the scale of the layer
     * @param paddingX amount of extra cells on the x-axis to iterate through. This handles for -x &
     *   +x directions.
     * @param paddingY amount of extra cells on the y-axis to iterate through. This handles for -y &
     *   +y directions.
     */
    inline fun forEachTileInView(
        viewBounds: Rect,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        paddingX: Int = 0,
        paddingY: Int = 0,
        action: (cx: Int, cy: Int) -> Unit,
    ) {
        val cellSize = cellSize * scale
        val pxTotalOffsetX = pxTotalOffsetX * scale
        val pxTotalOffsetY = pxTotalOffsetY * scale
        val minX = max(0, ((viewBounds.x - x - pxTotalOffsetX) / cellSize).toInt() - paddingX)
        val maxX =
            min(gridWidth - 1, ((viewBounds.x2 - x - pxTotalOffsetX) / cellSize).toInt() + paddingX)
        val minY = max(0, ((viewBounds.y - y - pxTotalOffsetY) / cellSize).toInt() - paddingY - 1)
        val maxY =
            min(
                gridHeight - 1,
                ((viewBounds.y2 - y - pxTotalOffsetY) / cellSize).toInt() + paddingY + 1
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
