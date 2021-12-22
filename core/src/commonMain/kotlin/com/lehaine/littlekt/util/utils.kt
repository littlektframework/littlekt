package com.lehaine.littlekt.util

/**
 * @author Colton Daily
 * @date 12/8/2021
 */
expect fun Double.toString(precision: Int): String

fun Float.toString(precision: Int): String = this.toDouble().toString(precision)

inline fun <T> List<T>.forEachReversed(action: (T) -> Unit): Unit {
    for (i in lastIndex downTo 0) {
        action(this[i])
    }
}
