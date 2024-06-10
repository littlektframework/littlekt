package com.littlekt.util

expect fun Double.toString(precision: Int): String

fun Float.toString(precision: Int): String = this.toDouble().toString(precision)

fun <T> MutableList<T>.truncate(newSize: Int) {
    check(newSize > 0) { "'newSize' must be >= 0: $newSize" }
    if (size <= newSize) return
    subList(newSize, size).clear()
}

expect fun epochMillis(): Long

expect fun now(): Double
