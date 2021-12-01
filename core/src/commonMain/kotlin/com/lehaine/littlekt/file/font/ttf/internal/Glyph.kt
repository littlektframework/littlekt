package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.Float32Buffer
import com.lehaine.littlekt.file.font.ttf.TtfFont
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class Glyph(
    var name: String? = null,
    unicode: Int? = null,
    unicodes: List<Int>? = null,
    val font: TtfFont,
    var index: Int,
    var xMin: Int = 0,
    var yMin: Int = 0,
    var xMax: Int = 0,
    var yMax: Int = 0,
    var advanceWidth: Float = 0f
) {
    var contors: Int = 0
    var codePoint: Int = -1
    var byteOffset: Int = 0
    var byteLength: Int = 0
    var vertices: Float32Buffer? = null
    var bounds: Rect? = null

    private val unicodesMut = mutableListOf<Int>().also {
        if (unicodes != null) {
            it.addAll(unicodes)
        }
    }

    var unicode: Int = unicode ?: 0
    val unicodes: List<Int> get() = unicodesMut

    fun addUnicode(unicode: Int) {
        if (unicodes.isEmpty()) {
            this.unicode = unicode
        }

        unicodesMut.add(unicode)
    }
}