package com.littlekt.graphics.g2d.font

/**
 * Holds data that describes a [Font] to be used for rendering glyphs.
 *
 * @see GlyphMetrics
 * @author Colton Daily
 * @date 1/5/2022
 */
data class FontMetrics(
    /** The size of the font */
    val size: Float = 0f,
    /** The max top for any character such as `É` */
    val top: Float = 0f,
    /** The ascent */
    val ascent: Float = 0f,
    /** The base line */
    val baseline: Float = 0f,
    /** The distance from one line of text to the next. */
    val lineHeight: Float = 0f,
    /** The descent */
    val descent: Float = 0f,
    /** The descent + line gap */
    val bottom: Float = 0f,
    /** Extra height */
    val leading: Float = 0f,
    /** The max width */
    val maxWidth: Float = 0f,
    val capHeight: Float = 0f,
    val padding: Padding = Padding(0, 0, 0, 0)
) {
    data class Padding(val top: Int, val right: Int, val bottom: Int, val left: Int)
}
