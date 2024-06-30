package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.geom.Angle

/**
 * A [FontCache] that provides additional render and text methods for a [BitmapFont].
 *
 * @author Colton Daily
 * @date 1/6/2022
 */
class BitmapFontCache(val font: BitmapFont) : FontCache(font.pages) {

    /**
     * Draws the text using the specified batch.
     *
     * @param batch the batch to draw with
     */
    fun draw(batch: Batch) {
        draw(batch, font.textures)
    }

    /**
     * Clears any existing glyphs of previous text and adds the new glyphs of the specified string
     * of [text].
     *
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param scaleX the scale of the x component of the glyphs
     * @param scaleY the scale of the y component of the glyphs
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
     * @param truncate the string to display when the text is too long to fit within [targetWidth]
     */
    fun setText(
        text: CharSequence,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        truncate: String? = null,
    ) =
        setText(
            font,
            text,
            x,
            y,
            scaleX,
            scaleY,
            rotation,
            color,
            targetWidth,
            align,
            wrap,
            truncate
        )

    /**
     * Adds new glyphs of the specified string of [text] on top of any existing glyphs.
     *
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param scaleX the scale of the x component of the glyphs
     * @param scaleY the scale of the y component of the glyphs
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
     * @param truncate the string to display when the text is too long to fit within [targetWidth]
     */
    fun addText(
        text: CharSequence,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        truncate: String? = null,
    ) =
        addText(
            font,
            text,
            x,
            y,
            scaleX,
            scaleY,
            rotation,
            color,
            targetWidth,
            align,
            wrap,
            truncate
        )
}
