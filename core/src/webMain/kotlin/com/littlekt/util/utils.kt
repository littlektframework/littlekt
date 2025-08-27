package com.littlekt.util

import com.littlekt.util.internal.clamp
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

expect fun <T> nativeGet(array :Array<T>, index: Int): T
expect fun <T> nativeSet(array: Array<T>, index: Int, element: T)
expect fun nativeSet(array: IntArray, index: Int, element: Int)
expect fun nativeSet(array: ByteArray, index: Int, element: Byte)
expect fun nativeIndexOf(array: IntArray, element: Int): Int
expect fun nativeGet(array :ShortArray, index: Int): Short
expect fun nativeGet(array : IntArray, index: Int): Int
expect fun nativeGet(array : FloatArray, index: Int): Float
expect fun nativeGetOrNull(array: FloatArray, index: Int): Float?
expect fun nativeGet(array : ByteArray, index: Int): Byte