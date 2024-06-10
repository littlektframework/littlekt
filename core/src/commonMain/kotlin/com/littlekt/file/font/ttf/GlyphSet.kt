package com.littlekt.file.font.ttf

import com.littlekt.file.ByteBuffer

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal typealias GlyphLoader = () -> MutableGlyph

internal fun SimpleGlyphLoader(index: Int, unitsPerEm: Int): GlyphLoader {
    return { MutableGlyph(index = index, unitsPerEm) }
}

internal fun TTfGlyphLoader(
    fontReader: TtfFontReader,
    index: Int,
    unitsPerEm: Int,
    parseGlyph: (MutableGlyph, ByteBuffer, Int) -> Unit,
    buffer: ByteBuffer,
    position: Int,
    buildPath: (GlyphSet, MutableGlyph) -> Unit,
): GlyphLoader {
    return {
        val glyph = MutableGlyph(index, unitsPerEm)
        glyph.calcPath = {
            parseGlyph.invoke(glyph, buffer, position)
            buildPath(fontReader.glyphs, glyph)
        }
        glyph
    }
}

internal class GlyphSet : Iterable<MutableGlyph> {
    private val glyphLoader = mutableMapOf<Int, GlyphLoader>()
    private val glyphs = mutableMapOf<Int, MutableGlyph>()
    private var length = 0

    val size
        get() = length

    override fun iterator(): Iterator<MutableGlyph> {
        return glyphs.values.iterator()
    }

    operator fun get(index: Int): MutableGlyph {

        val glyph =
            glyphs.getOrPut(index) {
                glyphLoader[index]?.invoke()
                    ?: error("Unable to retrieve or load glyph of index $index")
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
