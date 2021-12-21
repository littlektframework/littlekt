package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFontReader

/**
 * @author Colton Daily
 * @date 12/2/2021
 */
class TtfFont(val chars: CharArray) {
    constructor(chars: String = CharacterSets.LATIN_ALL) : this(chars.map { it }.toCharArray())

    private val glyphCache = mutableMapOf<Int, Glyph>()

    val glyphs: Map<Int, Glyph> get() = glyphCache
    val totalGlyphs get() = glyphs.size

    var ascender = 0
        private set
    var descender = 0
        private set
    var unitsPerEm = 1000
        private set

    fun load(data: ByteBuffer) {
        val buffer = createByteBuffer(data.toArray(), isBigEndian = true).also { it.flip() }
        val reader = TtfFontReader().also {
            it.parse(buffer)
            ascender = it.ascender
            descender = it.descender
            unitsPerEm = it.unitsPerEm
        }
        chars.forEach { char ->
            glyphCache[char.code] = reader[char]
        }
    }
}