package com.lehaine.littlekt.graphics.font

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
abstract class Font {
    /**
     * The name of the font or null.
     */
    var name: String? = null

    /**
     * The distance from one line of text to the next.
     */
    var lineHeight: Float = 0f

    /**
     * The distance from the top of most uppercase characters to the baseline.
     * Since the drawing position is the cap height of the first line, the cap height can be used to get
     * the location of the baseline.
     */
    var capHeight: Float = 1f

    /**
     * The distance from the cap height to the top of the tallest glyph.
     */
    var ascender: Float = 0f

    /**
     * The distance from the bottom of the glyph that extends the lowest to the baseline. This number is negative.
     */
    var descender: Float = 0f

    /**
     * The distance to move down when \n is encountered.
     */
    var down: Float = 0f

    /**
     * Multiplier for the line height of blank lines. down * blankLineHeight is used as the distance
     * to move down for a blank line.
     */
    var blankLineScale: Float = 1f

    /**
     * The glyph to display for characters not in the font or null.
     */
    var missingGlyph: Glyph? = null

    /**
     * The width of space character.
     */
    var spaceWidth: Float = 0f
}