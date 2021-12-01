package com.lehaine.littlekt.math

import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
data class Rect(var x: Float = 0f, var y: Float = 0f, var width: Float = 0f, var height: Float = 0f) {
    val isEmpty get() = width <= 0 || height <= 0
}

class RectBuilder {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    fun reset() {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
    }

    fun build() = Rect(minX, minY, maxX - minX, maxY - minY)

    fun include(x: Float, y: Float) {
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x)
        maxY = max(maxY, y)
    }
}