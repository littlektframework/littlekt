package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.font.Font
import com.lehaine.littlekt.graphics.font.Glyph
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.abs
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class GlyphLayout {
    val runs = mutableListOf<GlyphRun>()
    var width: Float = 0f
    var height: Float = 0f

    fun setText(
        font: Font,
        text: String,
        width: Float = 0f,
        scale: Float = 1f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        truncate: String? = null
    ) {
        if (text.isEmpty()) return

        val targetWidth = if (wrap) max(width, font.spaceWidth * 3 * scale) else width
        val wrapOrTruncate = wrap || truncate != null
        var lastRun = false
        var y = 0f
        var runStart = 0
        var currentRun: GlyphRun? = null

        run outer@{
            text.forEachIndexed { index, c ->
                var runEnd = 0
                var newLine = false

                if (index == text.length - 1) {
                    runEnd = text.length
                    lastRun = true
                } else if (c == '\n') {
                    runEnd = index
                    newLine = true
                }

                if (!newLine && !lastRun) return@forEachIndexed

                run runEnded@{
                    val run = GlyphRun().apply {
                        this.x = 0f
                        this.y = y
                        getGlyphsFrom(font, text, scale, runStart, runEnd, null)
                    } // TODO alloc and free from a pool instead
                    if (currentRun == null) {
                        currentRun = run.also { runs += it }
                    } else {
                        currentRun?.append(run)
                    }

                    if (wrapOrTruncate && (newLine || lastRun)) {
                        currentRun?.let { cr ->
                            var glyphRun = cr
                            var runWidth = glyphRun.advances.first() + glyphRun.advances[1]
                            var i = 2
                            while (i < glyphRun.advances.size) {
                                val glyph = glyphRun.glyphs[i - 1]
                                if (runWidth + glyph.advanceWidth * scale <= targetWidth) {
                                    runWidth += glyphRun.advances[i]
                                    i++
                                    continue
                                }

                                if (truncate != null) {
                                    TODO("Truncate not yet implemented")
                                    // return@outer
                                }

                                // wrap
                                var wrapIndex = font.wrapIndex(glyphRun.glyphs, i)
                                if ((wrapIndex == 0 && glyphRun.x == 0f) // requires at least one glyph per line
                                    || wrapIndex >= glyphRun.glyphs.size
                                ) { // wrap al least the glyph that didn't fit
                                    wrapIndex = i - 1
                                }
                                val newRun = wrap(font, scale, glyphRun, wrapIndex).also {
                                    currentRun = it
                                }?.also { glyphRun = it } ?: return@runEnded

                                runs += newRun
                                y += font.down
                                newRun.x = 0f
                                newRun.y = y

                                // start the wrap loop again, another wrap might be necessary
                                runWidth = newRun.advances.first() + newRun.advances[1]
                                i = 2
                            }
                        }
                    }
                }
                if (newLine) {
                    currentRun = null

                    y += if (runEnd == runStart) { // blank line
                        font.down * font.blankLineScale
                    } else {
                        font.down
                    }
                }
                runStart = index
            }
        }
        height = font.capHeight + abs(y)
        calculateWidths(scale)
        alignRuns(targetWidth, align)
    }

    private fun wrap(font: Font, scale: Float, first: GlyphRun, wrapIndex: Int): GlyphRun? {
        val glyphs2 = first.glyphs
        val glyphCount = first.glyphs.size
        val advances2 = first.advances

        // skip whitespace before the wrap index
        var firstEnd = wrapIndex
        while (firstEnd > 0) {
            if (!font.isWhitespace(glyphs2[firstEnd - 1].unicode.toChar())) {
                break
            }
            firstEnd--
        }

        // skip whitespace after the wrap index
        var secondStart = wrapIndex
        while (secondStart < glyphCount) {
            if (!font.isWhitespace(glyphs2[secondStart].unicode.toChar())) {
                break
            }
            secondStart++
        }

        // copy wrapped glyphs and advances to second run
        // the second run will contain the remaining glyph data, so swap instances rather than copying
        var second: GlyphRun? = null
        if (secondStart < glyphCount) {
            val newRun = GlyphRun()
            val glyphs1 = newRun.glyphs
            glyphs1.ensureCapacity(firstEnd)
            glyphs1.clear()
            for (i in 0 until firstEnd) {
                glyphs1 += glyphs2[i]
            }
            for (i in 0 until secondStart) {
                glyphs2.removeFirst()
            }
            first.glyphs = glyphs1
            newRun.glyphs = glyphs2

            val advances1 = newRun.advances
            advances1.size = firstEnd
            advances1.clear()
            for (i in 0 until firstEnd) {
                advances1 += advances2[i]
            }
            advances2.removeAt(1, secondStart)
            advances2[0] = glyphs2.first().advanceWidth * scale
            first.advances = advances1
            newRun.advances = advances2
            second = newRun
        }

        if (firstEnd == 0) {
            runs.removeLast()
        }
        return second
    }

    private fun calculateWidths(scale: Float) {
        var width = 0f
        runs.forEach { run ->
            var runWidth = run.x + run.advances.first()
            var max = 0f
            run.glyphs.forEachIndexed { index, glyph ->
                max = max(max, runWidth + glyph.advanceWidth * scale)
                runWidth += run.advances[index]
            }
            run.width = max(runWidth, max) - run.x
            width = max(width, run.x + run.width)
        }
        this.width = width
    }

    private fun alignRuns(targetWidth: Float, align: HAlign) {
        if (align != HAlign.LEFT) {
            val center = align == HAlign.CENTER
            runs.forEach { run ->
                run.x += if (center) 0.5f * (targetWidth - run.width) else targetWidth - run.width
            }
        }
    }
}

private fun Font.wrapIndex(glyphs: List<Glyph>, start: Int): Int {
    var i = start - 1
    var ch = glyphs[i].unicode.toChar()
    if (ch.isWhitespace) return i
    if (ch.isWrapChar) i--
    while (i > 0) {
        ch = glyphs[i].unicode.toChar()
        if (ch.isWhitespace || ch.isWrapChar) return i + 1
        i--
    }
    return 0
}

class GlyphRun {
    var glyphs = arrayListOf<Glyph>()
    var advances = FloatArrayList()
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f

    fun append(run: GlyphRun) {
        glyphs.addAll(run.glyphs)
        if (advances.isNotEmpty()) {
            advances.size--
        }
        advances.add(run.advances)
    }

    fun reset() {
        glyphs.clear()
        advances.clear()
    }

    fun getGlyphsFrom(font: Font, text: CharSequence, scale: Float, start: Int, end: Int, lastGlyph: Glyph?) {
        val max = end - start
        if (max == 0) return

        var i = start
        var currGlyph = lastGlyph
        glyphs.ensureCapacity(max)
        advances.size = max + 1
        advances.clear()

        do {
            val ch = text[i++]
            if (ch == '\r') continue
            var glyph = font[ch]
            if (glyph == null) {
                glyph = font.missingGlyph ?: continue
            }
            glyphs += glyph
            advances += glyph.advanceWidth * scale
            currGlyph = glyph
        } while (i < end)

        if (currGlyph != null) {
            advances += currGlyph.advanceWidth * scale
        }
    }

    override fun toString(): String {
        return buildString {
            append("\"")
            glyphs.forEach { append(it.unicode.toChar()) }
            append("\"")
            append(", $x, $y, $width")
        }
    }
}