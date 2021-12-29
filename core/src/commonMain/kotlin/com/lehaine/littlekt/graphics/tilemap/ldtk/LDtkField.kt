package com.lehaine.littlekt.graphics.tilemap.ldtk

/**
 * @author Colton Daily
 * @date 12/28/2021
 */

interface LDtkField<T>
data class LDtkValueField<T>(val value: T) : LDtkField<T>
data class LDtkArrayField<T>(val values: List<LDtkField<T>>) : LDtkField<T>