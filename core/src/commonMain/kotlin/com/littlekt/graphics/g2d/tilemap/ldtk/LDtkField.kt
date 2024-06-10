package com.littlekt.graphics.g2d.tilemap.ldtk

/**
 * A generic field in LDtk.
 *
 * @author Colton Daily
 * @date 12/28/2021
 */
interface LDtkField<T>

/** A generic data holder for the value of a [LDtkField]. */
data class LDtkValueField<T>(val value: T) : LDtkField<T>

/** A generic array data holder for a list of values of a [LDtkField]. */
data class LDtkArrayField<T>(val values: List<LDtkField<T>>) : LDtkField<T>
