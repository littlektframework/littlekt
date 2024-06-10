package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.Color

/**
 * An "IntGrid" layer of LDtk.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkIntGridLayer(
    private val intGridValueInfo: List<ValueInfo>,
    /** IntGrid integer values, map is based on coordIds */
    val intGrid: Map<Int, Int>,
    identifier: String,
    iid: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) :
    LDtkLayer(
        identifier,
        iid,
        type,
        cellSize,
        gridWidth,
        gridHeight,
        pxTotalOffsetX,
        pxTotalOffsetY,
        opacity
    ) {

    /**
     * Get the Integer value at selected coordinates
     *
     * @return -1 if none.
     */
    fun getInt(cx: Int, cy: Int): Int {
        return if (!isCoordValid(cx, cy) || !intGrid.contains(getCoordId(cx, cy))) {
            -1
        } else {
            intGrid[getCoordId(cx, cy)]
                ?: error("Selected coordinates are not valid for this IntGrid Layer")
        }
    }

    operator fun get(cx: Int, cy: Int): Int = getInt(cx, cy)

    /**
     * @param cx grid x coord
     * @param cy grid y coord
     * @param value optional parameter allows to check for a specific integer value
     * @return true if there is any value at selected coordinates.
     */
    fun hasValue(cx: Int, cy: Int, value: Int? = null): Boolean {
        return value == null && getInt(cx, cy) != 0 || value != null && getInt(cx, cy) == value
    }

    /**
     * Get the value String identifier at selected coordinates.
     *
     * @return null if none.
     */
    fun getName(cx: Int, cy: Int): String? {
        return if (!hasValue(cx, cy)) {
            null
        } else {
            intGridValueInfo[getInt(cx, cy) - 1].identifier
        }
    }

    /**
     * Get the value color ("#rrggbb" string format) at selected coordinates.
     *
     * @return null if none.
     */
    fun getColor(cx: Int, cy: Int): Color? {
        return if (!hasValue(cx, cy)) {
            null
        } else {
            Color.fromHex(intGridValueInfo[getInt(cx, cy) - 1].color)
        }
    }

    override fun toString(): String {
        return "LDtkIntGridLayer(valueInfos=$intGridValueInfo)"
    }

    data class ValueInfo(val identifier: String?, val color: String)
}
