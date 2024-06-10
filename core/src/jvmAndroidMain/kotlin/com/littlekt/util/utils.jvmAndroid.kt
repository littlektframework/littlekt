package com.littlekt.util

import com.littlekt.util.internal.clamp
import java.util.*

actual fun Double.toString(precision: Int): String =
    String.format(Locale.ENGLISH, "%.${precision.clamp(0, 12)}f", this)

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun now(): Double = System.nanoTime() / 1e6
