package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFontReader

/**
 * @author Colton Daily
 * @date 12/2/2021
 */
class TtfFont(val chars: CharSequence = CharacterSets.LATIN_ALL) : Font() {

    private val glyphCache = mutableMapOf<Int, Glyph>()

    override val glyphs: Map<Int, Glyph> get() = glyphCache

    fun load(data: ByteBuffer) {
        val buffer = createByteBuffer(data.toArray(), isBigEndian = true).also { it.flip() }
        val reader = TtfFontReader().also {
            it.parse(buffer)
            ascender = it.ascender.toFloat()
            descender = it.descender.toFloat()
            down = ascender
            unitsPerEm = it.unitsPerEm
        }
        chars.forEach { char ->
            glyphCache[char.code] = reader[char]
        }
    }
}