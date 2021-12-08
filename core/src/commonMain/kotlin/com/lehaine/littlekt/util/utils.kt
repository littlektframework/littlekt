package com.lehaine.littlekt.util

/**
 * @author Colton Daily
 * @date 12/8/2021
 */
expect fun Double.toString(precision: Int): String

fun Float.toString(precision: Int): String = this.toDouble().toString(precision)