package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 12/28/2021
 */

data class LDtkEnum(val name: String, val values: Map<String, LDtkEnumValue>) {
    operator fun get(value: String?) = values[value]
}

data class LDtkEnumValue(val name: String, val color: Color)