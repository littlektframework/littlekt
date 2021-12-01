package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFont

/**
 * @author Colton Daily
 * @date 12/1/2021
 */

internal typealias GlyphLoader = () -> Glyph

internal fun TTfGlyphLoader(
    font: TtfFont,
    index: Int,
    parseGlyph: () -> Unit,
    buffer: MixedBuffer,
    position: Int,
): GlyphLoader {
    return {
        val glyph = Glyph(index = index, font = font)



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
        val glyph = glyphs[index] ?: glyphLoader[index]?.invoke()
        check(glyph != null) { "Unable to retrieve or load glyph of index $index!" }
        return glyph
    }

    operator fun set(index: Int, loader: GlyphLoader) {
        glyphLoader[index] = loader
        length++
    }
}