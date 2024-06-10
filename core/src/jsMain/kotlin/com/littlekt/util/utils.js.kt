package com.littlekt.util

import com.littlekt.util.internal.clamp
import kotlin.js.Date
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

actual fun Double.toString(precision: Int): String {
    val p = precision.clamp(0, 12)
    val s = if (this < 0) "-" else ""
    var a = abs(this)

    if (p == 0) {
        return "$s${a.roundToLong()}"
    }

    val fac = 10.0.pow(p).roundToLong()
    var fracF = ((a % 1.0) * fac).roundToLong()
    if (fracF == fac) {
        fracF = 0
        a += 1
    }

    var frac = fracF.toString()
    while (frac.length < p) {
        frac = "0$frac"
    }
    return "$s${a.toLong()}.$frac"
}

actual fun epochMillis(): Long = Date.now().toLong()

actual fun now(): Double = js("performance.now()") as Double
