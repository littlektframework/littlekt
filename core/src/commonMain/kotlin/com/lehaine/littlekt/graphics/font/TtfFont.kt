package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFontReader
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/2/2021
 */
class TtfFont(val chars: CharSequence = CharacterSets.LATIN_ALL) : Font {

    private val glyphMetricsCache = mutableMapOf<Int, GlyphMetrics>()
    private val glyphCache = mutableMapOf<Int, TtfGlyph>()
    private var _metrics = FontMetrics()

    override val glyphMetrics: Map<Int, GlyphMetrics> get() = glyphMetricsCache
    val glyphs: Map<Int, TtfGlyph> get() = glyphCache
    override var wrapChars: CharSequence = ""

    var unitsPerEm: Int = 0

    override val metrics: FontMetrics get() = _metrics

    fun load(data: ByteBuffer) {
        val buffer = createByteBuffer(data.toArray(), isBigEndian = true).also { it.flip() }
        val scale: Float
        val reader = TtfFontReader().also {
            it.parse(buffer)
            scale = 1f / it.unitsPerEm
            _metrics = FontMetrics(
                1f,
                it.yMax * scale,
                ascent = it.ascender * scale,
                baseline = 0f,
                descent = it.descender * scale,
                bottom = it.descender * scale,
                leading = it.lineGap * scale,
                maxWidth = it.advanceWidthMax * scale,
                capHeight = it.capHeight.toFloat()
            )
            unitsPerEm = it.unitsPerEm
        }
        chars.forEach { char ->
            reader[char].let {
                glyphCache[char.code] = it
                glyphMetricsCache[char.code] = it.toGlyphMetric(scale)
            }
        }
    }

    private fun TtfGlyph.toGlyphMetric(scale: Float) =
        GlyphMetrics(
            1f,
            unicode,
            Rect.fromBounds(xMin * scale, yMin * scale, xMax * scale, yMax * scale),
            advanceWidth * scale
        )
}