package com.lehaine.littlekt.util.internal

import java.util.*

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
internal actual fun epochMillis(): Long = System.currentTimeMillis()

internal actual fun Double.toString(precision: Int): String =
    java.lang.String.format(Locale.ENGLISH, "%.${precision.clamp(0, 12)}f", this)