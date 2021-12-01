package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFont
import com.lehaine.littlekt.file.font.ttf.internal.*

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class GlyfParser(
    val buffer: MixedBuffer,
    val start: Int,
    val loca: IntArray,
    val font: TtfFont,
    useLowMemory: Boolean = false
) {

    fun parse(): GlyphSet {
        val glyphs = GlyphSet(font)
        for (i in 0 until loca.size - 1) {
            val offset = loca[i]
            val nextOffset = loca[i + 1]

            if (offset != nextOffset) {
                glyphs[i] = TTfGlyphLoader(font, i, ::parseGlyph, buffer, start + offset)
            } else {
                glyphs[i] = SimpleGlyphLoader(font, i)
            }
        }
        return glyphs
    }

    fun parseGlyph(glyph: Glyph, buffer: MixedBuffer, start: Int) {
        val p = Parser(buffer, start)
        glyph.contors = p.parseInt16.toInt()
        glyph.xMin = p.parseInt16.toInt()
        glyph.yMin = p.parseInt16.toInt()
        glyph.xMax = p.parseInt16.toInt()
        glyph.yMax = p.parseInt16.toInt()
    }
}