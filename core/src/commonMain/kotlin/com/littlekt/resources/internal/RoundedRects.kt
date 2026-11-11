package com.littlekt.resources.internal

import com.littlekt.graphics.Pixmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal val ROUNDED_RECT_RADII = intArrayOf(0, 2, 3, 4, 6, 8)
internal val ROUNDED_RECT_BORDERS = intArrayOf(0, 1, 2)

internal fun roundedRectName(radius: Int, border: Int) = "rounded_rect_${radius}_$border"

/** The size of each side of a rounded rect nine patch made by [roundedRectPixmap]. */
internal fun roundedRectCorner(radius: Int, border: Int) = max(max(radius, border), 1)

/**
 * Creates a white anti-aliased rounded rect [Pixmap] to be used as a nine patch with
 * [roundedRectCorner] on each side. When [border] is greater than `0` only the border is drawn.
 */
internal fun roundedRectPixmap(radius: Int, border: Int): Pixmap {
    val corner = roundedRectCorner(radius, border)
    val size = corner * 2 + 2
    val pixmap = Pixmap(size, size)
    val half = size * 0.5f
    val innerRadius = max(radius - border, 0).toFloat()
    for (y in 0 until size) {
        for (x in 0 until size) {
            val px = x + 0.5f
            val py = y + 0.5f
            var coverage = coverage(px, py, half, half, radius.toFloat())
            if (border > 0) {
                coverage -=
                    coverage(px, py, half - border, half - border, innerRadius, half, half)
            }
            val alpha = (max(coverage, 0f) * 255f + 0.5f).toInt()
            val index = (x + y * size) * 4
            pixmap.pixels[index] = 255.toByte()
            pixmap.pixels[index + 1] = 255.toByte()
            pixmap.pixels[index + 2] = 255.toByte()
            pixmap.pixels[index + 3] = alpha.toByte()
        }
    }
    return pixmap
}

private fun coverage(
    px: Float,
    py: Float,
    halfWidth: Float,
    halfHeight: Float,
    radius: Float,
    centerX: Float = halfWidth,
    centerY: Float = halfHeight,
): Float {
    if (halfWidth <= 0f || halfHeight <= 0f) return 0f
    val qx = abs(px - centerX) - (halfWidth - radius)
    val qy = abs(py - centerY) - (halfHeight - radius)
    val ox = max(qx, 0f)
    val oy = max(qy, 0f)
    val distance = sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - radius
    return (0.5f - distance).coerceIn(0f, 1f)
}
