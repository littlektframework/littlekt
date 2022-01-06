package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
interface Font : Disposable {
    val metrics: FontMetrics

    /**
     * The loaded glyphs of this [Font].
     */
    abstract val glyphMetrics: Map<Int, GlyphMetrics>

    /**
     * Additional characters besides whitespace where text is wrapped. Eg: a hyphen (-).
     */
    var wrapChars: CharSequence

    fun isWhitespace(char: Char) = when (char) {
        '\n', '\r', '\t', ' ' -> true
        else -> false
    }

    fun isWrapChar(char: Char): Boolean {
        if (wrapChars.isEmpty()) return false
        wrapChars.forEach {
            if (it == char) return true
        }
        return false
    }

    operator fun get(code: Int) = glyphMetrics[code]
    operator fun get(char: Char) = glyphMetrics[char.code]

    override fun dispose() = Unit
}