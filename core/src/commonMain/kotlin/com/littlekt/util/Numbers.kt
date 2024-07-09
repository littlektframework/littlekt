package com.littlekt.util

/**
 * Align this value with the given [alignment] by rounding this value up to the next multiplier of
 * [alignment] and return the new value.
 */
fun Int.align(alignment: Int): Int {
    val divideAndCeil = this / alignment + if (this % alignment == 0) 0 else 1
    return this * divideAndCeil
}

/**
 * Align this value with the given [alignment] by rounding this value up to the next multiplier of
 * [alignment] and return the new value.
 */
fun Long.align(alignment: Long): Long {
    val divideAndCeil = this / alignment + if (this % alignment == 0L) 0L else 1L
    return this * divideAndCeil
}
