package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.util.internal.insert
import kotlin.math.max

/**
 * @author Colt Daily
 * @date 1/5/22
 */
class BitmapFont(
    val fontSize: Float,
    val lineHeight: Float,
    val base: Float,
    val glyphs: Map<Int, Glyph>,
    val kernings: Map<Int, Kerning>
) : Font {
    /**
     * The name of the font or null.
     */
    var name: String? = null

    /**
     * The width of space character.
     */
    var spaceWidth: Float = 0f

    override val metrics: FontMetrics = run {
        val ascent = base
        val baseline = 0f
        val descent = lineHeight - base
        FontMetrics(
            fontSize, ascent, ascent, baseline, -descent, -descent, 0f,
            maxWidth = run {
                var width = 0f
                for (glyph in glyphs.values) width = max(width, glyph.slice.width.toFloat())
                width
            }
        )
    }

    override val glyphMetrics: Map<Int, GlyphMetrics>
        get() = TODO("Not yet implemented")

    override var wrapChars: CharSequence = ""

    private val slices = mutableListOf<TextureSlice>()
    private val cache = FontCache()


    data class Glyph(
        val fontSize: Float,
        val id: Int,
        val slice: TextureSlice,
        val xoffset: Int,
        val yoffset: Int,
        val xadvance: Int
    )

    class Kerning(
        val first: Int,
        val second: Int,
        val amount: Int
    ) {
        companion object {
            fun buildKey(f: Int, s: Int) = 0.insert(f, 0, 16).insert(s, 16, 16)
        }
    }
}
