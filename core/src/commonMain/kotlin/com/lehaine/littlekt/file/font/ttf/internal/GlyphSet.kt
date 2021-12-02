package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFont

/**
 * @author Colton Daily
 * @date 12/1/2021
 */

internal typealias GlyphLoader = () -> Glyph

internal fun SimpleGlyphLoader(font: TtfFont, index: Int): GlyphLoader {
    return {
        Glyph(index = index, font = font)
    }
}

internal fun TTfGlyphLoader(
    font: TtfFont,
    index: Int,
    parseGlyph: (Glyph, MixedBuffer, Int) -> Unit,
    buffer: MixedBuffer,
    position: Int,
    buildPath: (GlyphSet, Glyph) -> Unit,
): GlyphLoader {
    return {
        val glyph = Glyph(index = index, font = font)
        glyph.calcPath = {
            parseGlyph.invoke(glyph, buffer, position)
            buildPath(font.glyphs, glyph)
        }
        glyph
    }
}

internal class GlyphSet(val font: TtfFont) : Iterable<Glyph> {
    private val glyphLoader = mutableMapOf<Int, GlyphLoader>()
    private val glyphs = mutableMapOf<Int, Glyph>()
    private var length = 0

    val size get() = length

    override fun iterator(): Iterator<Glyph> {
        return glyphs.values.iterator()
    }

    operator fun get(index: Int): Glyph {

        val glyph = glyphs.getOrPut(index) {
            glyphLoader[index]?.invoke() ?: error("Unable to retrieve or load glyph of index $index")
        }
        return glyph
    }

    operator fun set(index: Int, loader: GlyphLoader) {
        glyphLoader[index] = loader
        length++
    }

    override fun toString(): String {
        return "GlyphSet(glyphs=$glyphs, length=$length)"
    }


}