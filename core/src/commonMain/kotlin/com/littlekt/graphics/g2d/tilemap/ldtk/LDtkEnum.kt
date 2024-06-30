package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.Color

/**
 * A generic data holder for LDtk enums.
 *
 * @param name the enums name
 * @param values the key-value map of values for the given enum.
 * @author Colton Daily
 * @date 12/28/2021
 */
data class LDtkEnum(val name: String, val values: Map<String, LDtkEnumValue>) {
    operator fun get(value: String?) = values[value]
}

/**
 * A value of a [LDtkEnum].
 *
 * @param name the value name
 * @param color the color of the enum in LDtk
 */
data class LDtkEnumValue(val name: String, val color: Color)
