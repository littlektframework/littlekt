package com.littlekt.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A rectangle that has a given position and size.
 *
 * @author Colton Daily
 * @date 11/30/2021
 */
open class Rect(
    /** The x-position of the rect. */
    var x: Float = 0f,
    /** The y-position of the rect. */
    var y: Float = 0f,
    /** The width of the rect. */
    var width: Float = 0f,
    /** The height of the rect. */
    var height: Float = 0f,
) {

    /** The right-most x-coordinate. This is the same as doing `x + width`. */
    val x2: Float
        get() = x + width

    /** The top-most y-coordinate. This is the same as doing `y + height`. */
    val y2: Float
        get() = y + height

    /** Determines if either the [width] or [height] are <= 0. */
    val isEmpty: Boolean
        get() = width <= 0 || height <= 0

    /** Set the coordinates and size of this [Rect]. */
    fun set(newX: Float, newY: Float, newWidth: Float, newHeight: Float): Rect {
        x = newX
        y = newY
        width = newWidth
        height = newHeight
        return this
    }

    /** @return true if this [Rect] intersects with the target [rect]. */
    fun intersects(rect: Rect) = intersects(left = rect.x, rect.y, rect.x2, rect.y2)

    /**
     * @param left the left-most coordinates
     * @param bottom the bottom-most coordinates
     * @param right the right=most coordinates
     * @param top the top-most coordinates
     * @return true if this [Rect] intersects the given rectangle at the specified corner
     *   coordinates.
     */
    fun intersects(left: Float, bottom: Float, right: Float, top: Float): Boolean =
        !(x >= right || x2 <= left || y >= top || y2 <= bottom)

    override fun toString(): String {
        return "Rect(x=$x, y=$y, width=$width, height=$height, x2=$x2, y2=$y2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Rect

        if (x != other.x) return false
        if (y != other.y) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        return result
    }

    companion object {
        /**
         * Create a new [Rect] given the specified bounds.
         *
         * @param x the left-most x-coordinate.
         * @param y the bottom-most y-coordinate.
         * @param x2 the right-most x-coordinate. i.e. `x + width`.
         * @param y2 the top-most y-coordinate. i.e. `y + height`.
         */
        fun fromBounds(x: Float, y: Float, x2: Float, y2: Float) = Rect(x, y, x2 - x, y2 - y)
    }
}

class RectBuilder {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    fun isEmpty() =
        minX == Float.POSITIVE_INFINITY ||
            minY == Float.POSITIVE_INFINITY ||
            maxX == Float.NEGATIVE_INFINITY ||
            maxY == Float.NEGATIVE_INFINITY

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
    fun addBezier(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x: Float,
        y: Float,
    ) {
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
        return (1 - t).pow(3) * v0 +
            3 * (1 - t).pow(2) * t * v1 +
            3 * (1 - t) * t.pow(2) * v2 +
            t.pow(3) * v3
    }

    private operator fun Pair<Float, Float>.get(index: Int) = if (index == 0) first else second
}
