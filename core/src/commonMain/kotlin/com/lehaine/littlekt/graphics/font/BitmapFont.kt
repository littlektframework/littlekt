package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
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
    val textures: List<Texture>,
    val glyphs: Map<Int, Glyph>,
    val kernings: Map<Int, Kerning>
) : Font {

    private val slices = glyphs
    private val cache = FontCache()

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

    override val glyphMetrics: Map<Int, GlyphMetrics> = glyphs.entries.map {
        val glyph = it.value
        GlyphMetrics(
            fontSize,
            glyph.id,
            Rect(
                glyph.xoffset.toFloat(),
                glyph.yoffset.toFloat(),
                glyph.slice.width.toFloat(),
                glyph.slice.height.toFloat()
            ),
            glyph.xadvance.toFloat(),
            u = glyph.slice.u,
            v = glyph.slice.v,
            u2 = glyph.slice.u2,
            v2 = glyph.slice.v2
        )
    }.associateBy { it.code }

    override var wrapChars: CharSequence = ""

    fun draw(
        batch: SpriteBatch,
        text: CharSequence,
        x: Float,
        y: Float,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        cache.setText(this, text, x, y, 1f, rotation, color, targetWidth, align, wrap)
        cache.draw(batch, textures[0]) // TODO impl multiple page font
    }

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
