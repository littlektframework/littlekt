package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.Uint8Buffer
import com.lehaine.littlekt.file.createMixedBuffer
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

    var fontSize = 72
        set(value) {
            field = value
//            glyphs.values.forEach {
//                it.path.recalculate(fontSize = field)
//            }
        }

    fun load(data: Uint8Buffer) {
        val buffer = createMixedBuffer(data.toArray(), isBigEndian = true).also { it.flip() }
        val reader = TtfFontReader().also {
            it.parse(buffer)
            ascender = it.ascender
            descender = it.descender
            unitsPerEm = it.unitsPerEm
        }
        chars.forEach { char ->
            glyphCache[char.code] = reader[char]//.also { it.path.recalculate(fontSize = fontSize) }
        }
    }
}