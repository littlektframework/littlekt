package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.Rect
import com.littlekt.math.geom.Angle
import kotlin.math.max
import kotlin.math.min

/**
 * A [Font] that handles rendering of bitmap fonts using the **BMFont** text format.
 *
 * The text is drawn using a [SpriteBatch]. The text can also be cached in a [FontCache] to render
 * static text without having to compute the geometry of each glyph every frame. The
 * [BitmapFontCache] can be used specifically for this class.
 *
 * The textures required for a [BitmapFont] are managed and may be disposed by directly calling the
 * [release] method on the [BitmapFont] object.
 *
 * @author Colt Daily
 * @date 1/5/22
 */
class BitmapFont(
    val fontSize: Float,
    val lineHeight: Float,
    val base: Float,
    val capHeight: Float,
    val padding: FontMetrics.Padding,
    override val textures: List<Texture>,
    val glyphs: Map<Int, Glyph>,
    val kernings: Map<Int, Kerning>,
    val pages: Int = 1,
) : TextureFont {

    private val cache = BitmapFontCache(this)

    /** The name of the font or null. */
    var name: String? = null

    override val metrics: FontMetrics = run {
        val ascent = base - capHeight
        var descent = 0f
        var maxWidth = 0f
        for (glyph in glyphs.values) {
            maxWidth = max(maxWidth, glyph.slice.width.toFloat())
            if (glyph.width > 0 && glyph.height > 0) {
                descent = min(base + glyph.yoffset, descent)
            }
        }
        FontMetrics(
            size = fontSize,
            top = ascent,
            ascent = ascent,
            baseline = base,
            lineHeight = lineHeight,
            descent = descent,
            bottom = descent,
            leading = 0f,
            maxWidth = maxWidth,
            capHeight = capHeight,
            padding = padding
        )
    }

    override val glyphMetrics: Map<Int, GlyphMetrics> =
        glyphs.entries
            .map {
                val glyph = it.value
                GlyphMetrics(
                    size = fontSize,
                    code = glyph.id,
                    bounds =
                        Rect(
                            glyph.xoffset.toFloat(),
                            glyph.yoffset.toFloat(),
                            glyph.width.toFloat(),
                            glyph.height.toFloat()
                        ),
                    xAdvance = glyph.xadvance.toFloat(),
                    u0 = glyph.slice.u,
                    v0 = glyph.slice.v,
                    u1 = glyph.slice.u1,
                    v1 = glyph.slice.v1,
                    page = glyph.page
                )
            }
            .associateBy { it.code }

    override var wrapChars: CharSequence = ""

    override fun getKerning(first: Int, second: Int): Kerning? {
        return kernings[Kerning.buildKey(first, second)]
    }

    /**
     * Draws a string of text.
     *
     * @param batch the batch to draw the text with
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
     * @param scaleX the x-scale of the text
     * @param scaleY the y-scale of the text
     * @param letterSpacing the extra space, in pixels, added between each glyph of a line
     * @param lineSpacing the extra space, in pixels, added between each line
     * @param justify if `true`, wrapped lines are stretched to [targetWidth] by widening their
     *   spaces
     * @see [FontCache.setText]
     * @see [FontCache.draw]
     */
    fun draw(
        batch: Batch,
        text: CharSequence,
        x: Float,
        y: Float,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        letterSpacing: Float = 0f,
        lineSpacing: Float = 0f,
        justify: Boolean = false,
    ) {
        cache.setText(
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
            letterSpacing = letterSpacing,
            lineSpacing = lineSpacing,
            justify = justify,
        )
        cache.draw(batch)
    }

    override fun release() {
        textures.forEach { it.release() }
    }

    data class Glyph(
        val fontSize: Float,
        val id: Int,
        val slice: TextureSlice,
        val xoffset: Int,
        val yoffset: Int,
        val xadvance: Int,
        val width: Int,
        val height: Int,
        val page: Int,
    )
}
