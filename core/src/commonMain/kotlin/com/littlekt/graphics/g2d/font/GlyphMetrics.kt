package com.littlekt.graphics.g2d.font

import com.littlekt.math.Rect

/**
 * Metrics that describe a single glyph which can be used for rendering.
 *
 * @see FontMetrics
 * @see Font
 * @author Colton Daily
 * @date 1/5/2022
 */
data class GlyphMetrics(
    val size: Float = 0f,
    val code: Int = 0,
    val bounds: Rect = Rect(),
    val xAdvance: Float = 0f,
    val u0: Float = 0f,
    val v0: Float = 0f,
    val u1: Float = 0f,
    val v1: Float = 0f,
    val page: Int = 0
) {
    val left: Float
        get() = bounds.x

    val right: Float
        get() = bounds.x2

    val top: Float
        get() = bounds.y2

    val bottom: Float
        get() = bounds.y

    val width: Float
        get() = bounds.width

    val height: Float
        get() = bounds.height
}
