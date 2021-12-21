package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.IntGridValueDefinition
import com.lehaine.littlekt.file.ldtk.LayerInstance

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkIntGridLayer(
    intGridValues: List<IntGridValueDefinition>,
    json: LayerInstance
) : LDtkLayer(json) {

    data class ValueInfo(val identifier: String?, val color: Int)

    private val valueInfos = intGridValues.map {
        ValueInfo(it.identifier, it.color.substring(1).toInt(16))
    }
    private val _intGrid = mutableMapOf<Int, Int>().apply {
        if (json.intGridCSV != null) {
            json.intGridCSV.forEachIndexed { index, i ->
                put(index, i)
            }
        } else {
            json.intGrid?.forEach {
                put(it.coordID, it.v)
            }
        }
    }

    /**
     * IntGrid integer values, map is based on coordIds
     */
    val intGrid = _intGrid.toMap()

    /**
     * Get the Integer value at selected coordinates
     * @return -1 if none.
     */
    fun getInt(cx: Int, cy: Int): Int {
        return if (!isCoordValid(cx, cy) || !intGrid.contains(getCoordId(cx, cy))) {
            -1
        } else {
            intGrid[getCoordId(cx, cy)] ?: error("Selected coordinates are not valid for this IntGrid Layer")
        }
    }

    /**
     * @param cx grid x coord
     * @param cy grid y coord
     * @param value optional parameter allows to check for a specific integer value
     * @return true if there is any value at selected coordinates.
     */
    fun hasValue(cx: Int, cy: Int, value: Int? = null): Boolean {
        return value == null && getInt(cx, cy) != 0 ||
                value != null && getInt(cx, cy) == value
    }

    /**
     * Get the value String identifier at selected coordinates.
     * @return null if none.
     */
    fun getName(cx: Int, cy: Int): String? {
        return if (!hasValue(cx, cy)) {
            null
        } else {
            valueInfos[getInt(cx, cy) - 1].identifier
        }
    }

    /**
     * Get the value color (0xrrggbb Unsigned-Int format) at selected coordinates.
     * @return null if none.
     */
    fun getColorInt(cx: Int, cy: Int): Int? {
        return if (!hasValue(cx, cy)) {
            null
        } else {
            valueInfos[getInt(cx, cy) - 1].color
        }
    }

    /**
     * Get the value color ("#rrggbb" string format) at selected coordinates.
     * @return null if none.
     */
    fun getColorHex(cx: Int, cy: Int): String? {
        return if (!hasValue(cx, cy)) {
            null
        } else {
            "#000000" // TODO
            //     Project.intToHex(valueInfos[getInt(cx, cy) - 1].color)
        }
    }

    override fun toString(): String {
        return "LDtkIntGridLayer(valueInfos=$valueInfos)"
    }


}