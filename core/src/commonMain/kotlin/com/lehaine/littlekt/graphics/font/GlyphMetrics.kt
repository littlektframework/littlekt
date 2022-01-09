package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.math.Rect

/**
 * Metrics that describe a single glyph which can be used for rendering.
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
    val u: Float = 0f,
    val v: Float = 0f,
    val u2: Float = 0f,
    val v2: Float = 0f,
    val page: Int = 0
) {
    val left: Float get() = bounds.x
    val right: Float get() = bounds.x2
    val top: Float get() = bounds.y
    val bottom: Float get() = bounds.y2
    val width: Float get() = bounds.width
    val height: Float get() = bounds.height
}