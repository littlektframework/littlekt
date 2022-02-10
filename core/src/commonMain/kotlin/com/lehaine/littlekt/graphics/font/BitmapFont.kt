package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.max

/**
 * A [Font] that handles rendering of bitmap fonts using the **BMFont** text format.
 *
 * The text is drawn using a [SpriteBatch]. The text can also be cached in a [FontCache] to render static text without
 * having to compute the geometry of each glyph every frame. The [BitmapFontCache] can be used specifically for this class.
 *
 * The textures required for a [BitmapFont] are managed and may be disposed by directly calling the [dispose] method
 * on the [BitmapFont] object.
 * @author Colt Daily
 * @date 1/5/22
 */
class BitmapFont(
    val fontSize: Float,
    val lineHeight: Float,
    val base: Float,
    val capHeight: Float,
    val textures: List<Texture>,
    val glyphs: Map<Int, Glyph>,
    val kernings: Map<Int, Kerning>,
    val pages: Int = 1
) : Font {

    private val cache = BitmapFontCache(this)

    /**
     * The name of the font or null.
     */
    var name: String? = null


    override val metrics: FontMetrics = run {
        val ascent = base
        val baseline = 0f
        val descent = lineHeight - base
        FontMetrics(
            size = fontSize,
            top = ascent,
            ascent = ascent,
            baseline = baseline,
            descent = -descent,
            bottom = -descent,
            leading = 0f,
            maxWidth = run {
                var width = 0f
                for (glyph in glyphs.values) width = max(width, glyph.slice.width.toFloat())
                width
            },
            capHeight = capHeight
        )
    }

    override val glyphMetrics: Map<Int, GlyphMetrics> = glyphs.entries.map {
        val glyph = it.value
        GlyphMetrics(
            size = fontSize,
            code = glyph.id,
            bounds = Rect(
                glyph.xoffset.toFloat(),
                glyph.yoffset.toFloat(),
                glyph.slice.width.toFloat(),
                glyph.slice.height.toFloat()
            ),
            xAdvance = glyph.xadvance.toFloat(),
            u = glyph.slice.u,
            v = glyph.slice.v,
            u2 = glyph.slice.u2,
            v2 = glyph.slice.v2,
            page = glyph.page
        )
    }.associateBy { it.code }

    override var wrapChars: CharSequence = ""

    override fun getKerning(first: Int, second: Int): Kerning? {
        return kernings[Kerning.buildKey(first, second)]
    }

    /**
     * Draws a string of text.
     * @param batch the batch to draw the text with
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
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
        wrap: Boolean = false
    ) {
        cache.setText(text, x, y, 1f, 1f, rotation, color, targetWidth, align, wrap)
        cache.draw(batch) // TODO impl multiple page font
    }

    override fun dispose() {
        textures.forEach {
            it.dispose()
        }
    }

    data class Glyph(
        val fontSize: Float,
        val id: Int,
        val slice: TextureSlice,
        val xoffset: Int,
        val yoffset: Int,
        val xadvance: Int,
        val page: Int,
    )

}
