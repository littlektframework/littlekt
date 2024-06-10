package com.littlekt.graphics.g2d.font

import com.littlekt.Releasable

/**
 * An interface for describing and creating fonts in order to render their glyphs.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
interface Font : Releasable {
    /** The font metrics of this [Font] */
    val metrics: FontMetrics

    /** The glyph metrics supported by this [Font]. */
    val glyphMetrics: Map<Int, GlyphMetrics>

    /** Additional characters besides whitespace where text is wrapped. Eg: a hyphen (-). */
    var wrapChars: CharSequence

    /**
     * Determines if the character is considered whitespace.
     *
     * @param char the character to check
     * @return `true` if it is a whitespace; `false` otherwise.
     */
    fun isWhitespace(char: Char) =
        when (char) {
            '\n',
            '\r',
            '\t',
            ' ' -> true
            else -> false
        }

    /**
     * Determines if the character is considered a "wrap" character.
     *
     * @param char the character to check
     * @return `true` if it is a "wrap" character; `false` otherwise.
     * @see wrapChars
     */
    fun isWrapChar(char: Char): Boolean {
        if (wrapChars.isEmpty()) return false
        wrapChars.forEach { if (it == char) return true }
        return false
    }

    /**
     * Fetches the [Kerning] between two codepoints.
     *
     * @param first the first codepoint of a character
     * @param second the second codepoint of a character
     * @return the kerning between the two codepoints if it exists; otherwise null
     */
    fun getKerning(first: Int, second: Int): Kerning?

    /**
     * Fetches the [Kerning] between two characters.
     *
     * @param first the first character
     * @param second the second character
     * @return the kerning between the two codepoints if it exists; otherwise null
     */
    fun getKerning(first: Char, second: Char): Kerning? = getKerning(first.code, second.code)

    /**
     * Fetches the [Kerning.amount] between two codepoint.
     *
     * @param scale the scale to multiply the resulting amount by
     * @param first the first codepoint
     * @param second the second codepoint
     * @return the kerning amount multiplied by the scale between the two codepoints if it exists;
     *   otherwise `0`
     */
    fun getKerningAmount(scale: Float, first: Int, second: Int): Float =
        getKerning(first, second)?.amount?.times(scale) ?: 0f

    /**
     * Fetches the [Kerning.amount] between two characters.
     *
     * @param scale the scale to multiply the resulting amount by
     * @param first the first character
     * @param second the second character
     * @return the kerning amount multiplied by the scale between the two codepoints if it exists;
     *   otherwise `0`
     */
    fun getKerningAmount(scale: Float, first: Char, second: Char): Float =
        getKerningAmount(scale, first.code, second.code)

    /** @return a [GlyphMetrics] with the specified [code] if it exists; otherwise `null` */
    operator fun get(code: Int) = glyphMetrics[code]

    /** @return a [GlyphMetrics] with the specified [char] if it exists; otherwise `null` */
    operator fun get(char: Char) = glyphMetrics[char.code]

    override fun release() = Unit
}
