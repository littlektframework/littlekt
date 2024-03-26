package com.lehaine.littlekt.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
open class Rect(var x: Float = 0f, var y: Float = 0f, var width: Float = 0f, var height: Float = 0f) {
    val x2 get() = x + width
    val y2 get() = y + height
    val isEmpty get() = width <= 0 || height <= 0

    fun set(newX: Float, newY: Float, newWidth: Float, newHeight: Float): Rect {
        x = newX
        y = newY
        width = newWidth
        height = newHeight
        return this
    }

    fun intersects(rect: Rect) = intersects(rect.x, rect.y, rect.x2, rect.y2)

    fun intersects(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        if (x >= right || left >= x2) {
            return false
        }

        if (y >= bottom || top >= y2) {
            return false
        }

        return true
    }

    /**
     * Returns true if all of the Rect()s in the list intersect with this rect
     */
    fun intersectsListAll(list: List<Rect>): Boolean {
        for(rect in list) {
            if(!this.intersects(rect)) { return false }
        }
        return true
    }

    /**
     * Returns true if any of the rectangles intersect with this one.
     */
    fun intersectsListAny(list: List<Rect>): Boolean {
        var foundIntersection = false
        for(rect in list) {
            if(this.intersects(rect)) { foundIntersection = true }
        }
        return foundIntersection
    }

    /**
     * Returns all of the Rect()s that intersect with this rect.
     */
    fun getIntersectingRects(list: List<Rect>): List<Rect> {
        var rectsFound = mutableListOf<Rect>()
        for(rect in list) {
            if(this.intersects(rect)) { rectsFound += rect }
        }
        return rectsFound
    }

    override fun toString(): String {
        return "Rect(x=$x, y=$y, width=$width, height=$height, x2=$x2, y2=$y2)"
    }

    companion object {
        fun fromBounds(x: Float, y: Float, x2: Float, y2: Float) = Rect(x, y, x2 - x, y2 - y)
    }
}

class RectBuilder {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    fun isEmpty() = minX == Float.POSITIVE_INFINITY || minY == Float.POSITIVE_INFINITY
            || maxX == Float.NEGATIVE_INFINITY || maxY == Float.NEGATIVE_INFINITY

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

    fun include(x: Int, y: Int) = include(x.toFloat(), y.toFloat())

    fun includeX(x: Float) {
        minX = min(minX, x)
        maxX = max(maxX, x)
    }

    fun includeY(y: Float) {
        minY = min(minY, y)
        maxY = max(maxY, y)
    }

    fun includeX(x: Int) = includeX(x.toFloat())
    fun includeY(y: Int) = includeY(y.toFloat())

    // http://nishiohirokazu.blogspot.com/2009/06/how-to-calculate-bezier-curves-bounding.html
    fun addBezier(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
        val p0 = x0 to y0
        val p1 = x1 to y1
        val p2 = x2 to y2
        val p3 = x to y
        include(x0, y0)
        include(x, y)

        for (i in 0..1) {
            val b = 6 * p0[i] - 12 * p1[i] + 6 * p2[i]
            val a = -3 * p0[i] + 9 * p1[i] - 9 * p2[i] + 3 * p3[i]
            val c = 3 * p1[i] - 3 * p0[i]
            if (a == 0f) {
                if (b == 0f) {
                    continue
                }
                val t = -c / b
                if (0f < t && t < 1f) {
                    if (i == 0) includeX(derive(p0[i], p1[i], p2[i], p3[i], t))
                    if (i == 2) includeY(derive(p0[i], p1[i], p2[i], p3[i], t))
                }
                continue
            }

            val b2ac = (b.toFloat().pow(2) - 4 * c * a).toInt()
            if (b2ac < 0) continue
            val t1 = (-b + sqrt(b2ac.toFloat())) / (2 * a)
            if (0f < t1 && t1 < 1f) {
                if (i == 0) includeX(derive(p0[i], p1[i], p2[i], p3[i], t1))
                if (i == 2) includeY(derive(p0[i], p1[i], p2[i], p3[i], t1))
            }
            val t2 = (-b - sqrt(b2ac.toFloat())) / (2 * a)
            if (0f < t2 && t2 < 1f) {
                if (i == 0) includeX(derive(p0[i], p1[i], p2[i], p3[i], t2))
                if (i == 2) includeY(derive(p0[i], p1[i], p2[i], p3[i], t2))
            }
        }
    }

    fun addQuad(x0: Float, y0: Float, x1: Float, y1: Float, x: Float, y: Float) {
        val cp1x = x0 + 2 / 3 * (x1 - x0)
        val cp1y = y0 + 2 / 3 * (y1 - y0)
        val cp2x = cp1x + 1 / 3 * (x - x0)
        val cp2y = cp1y + 1 / 3 * (y - y0)
        addBezier(x0, y0, cp1x, cp1y, cp2x, cp2y, x, y)
    }

    private fun derive(v0: Float, v1: Float, v2: Float, v3: Float, t: Float): Float {
        return (1 - t).pow(3) * v0 + 3 * (1 - t).pow(2) * t * v1 + 3 * (1 - t) * t.pow(2) * v2 + t.pow(3) * v3
    }

    private operator fun Pair<Float, Float>.get(index: Int) = if (index == 0) first else second
}