package com.littlekt.util

/** Align this value with the given [alignment] and return the new value. */
fun Int.align(alignment: Int): Int {
    var result = this
    while (result % alignment != 0) result++
    return result
}

/** Align this value with the given [alignment] and return the new value. */
fun Long.align(alignment: Long): Long {
    var result = this
    while (result % alignment != 0L) result++
    return result
}
