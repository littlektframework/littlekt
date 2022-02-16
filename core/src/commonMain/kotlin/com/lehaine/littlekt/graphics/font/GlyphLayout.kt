package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.abgr
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import com.lehaine.littlekt.util.datastructure.Pool
import com.lehaine.littlekt.util.truncate
import kotlin.math.abs
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class GlyphLayout {
    private var glyphCount: Int = 0

    private val _runs = mutableListOf<GlyphRun>()
    val runs: List<GlyphRun> get() = _runs

    private val _colors = mutableListOf<Int>()
    val colors: List<Int> get() = _colors

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set

    private val glyphRunPool = Pool(
        reset = {
            it.x = 0f
            it.y = 0f
            it.glyphs.clear()
            it.width = 0f
            it.color = Color.WHITE
            it.advances.clear()
        },
    ) {
        GlyphRun()
    }

    /**
     * Calculates the glyphs position and size.
     * @param font the font to use in the setting the text
     * @param text the character sequence of text
     * @param color the default color to use for the text
     * @param width the width to use for alignment, line wrapping, and truncation. May be zero if those features are not used.
     * @param scaleX the x-scale of the text
     * @param scaleY the y-scale of the text
     * @param wrap whether to wrap the text or not. Requires [width] to be set.
     * @param truncate if not null and the width of the glyphs exceed [width], the glyphs are truncated with the glyphs
     * of the specified truncate string. Truncate should not be used with text that contains multiple lines. Wrap is
     * ignored if truncate is not null.
     */
    fun setText(
        font: Font,
        text: CharSequence,
        color: Color = Color.WHITE,
        width: Float = 0f,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        truncate: String? = null
    ) {
        reset()

        if (text.isEmpty()) return

        val targetWidth = if (wrap) max(width, font.metrics.maxWidth * 3 * scaleX) else width
        val wrapOrTruncate = wrap || truncate != null
        var currentColor = color.abgr()
        val nextColor = currentColor
        _colors += 0
        _colors += currentColor
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
                    val run = glyphRunPool.alloc().apply {
                        this.x = 0f
                        this.y = y
                        getGlyphsFrom(font, text, scaleX, runStart, runEnd, null)
                    }
                    glyphCount += run.glyphs.size

                    if (nextColor != currentColor) {// TODO implement a markup
                        if (_colors[colors.size - 2] == glyphCount) {
                            _colors[colors.size - 1] = nextColor
                        } else {
                            _colors += glyphCount
                            _colors += nextColor
                        }
                        currentColor = nextColor
                    }

                    if (run.glyphs.isEmpty()) {
                        glyphRunPool.free(run)
                        if (currentRun == null) return@runEnded
                    } else if (currentRun == null) {
                        currentRun = run.also { _runs += it }
                    } else {
                        currentRun?.append(run)
                        glyphRunPool.free(run)
                    }

                    if (newLine || lastRun) {
                        currentRun?.setLastGlyphAdvanceToWidth(scaleX)
                    }
                    if (wrapOrTruncate && (newLine || lastRun)) {
                        currentRun?.let { cr ->
                            var glyphRun = cr
                            var runWidth = glyphRun.advances.first() + glyphRun.advances[1]
                            var i = 2
                            while (i < glyphRun.advances.size) {
                                val glyph = glyphRun.glyphs[i - 1]
                                if (runWidth + glyph.xAdvance * scaleX <= targetWidth) {
                                    runWidth += glyphRun.advances[i]
                                    i++
                                    continue
                                }

                                if (truncate != null) {
                                    truncate(font, cr, scaleX, width, truncate)
                                    return@outer
                                }

                                // wrap
                                var wrapIndex = font.wrapIndex(glyphRun.glyphs, i)
                                if ((wrapIndex == 0 && glyphRun.x == 0f) // requires at least one glyph per line
                                    || wrapIndex >= glyphRun.glyphs.size
                                ) { // wrap al least the glyph that didn't fit
                                    wrapIndex = i - 1
                                }
                                val newRun = wrap(font, scaleX, glyphRun, wrapIndex).also {
                                    currentRun = it
                                }?.also { glyphRun = it } ?: return@runEnded

                                _runs += newRun
                                y += font.metrics.lineHeight * scaleY
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
                        font.metrics.lineHeight * scaleY
                    } else {
                        font.metrics.lineHeight * scaleY
                    }
                }
                runStart = index
            }
        }
        height = font.metrics.capHeight * scaleY + abs(y)

        calculateWidths(scaleX)
        alignRuns(targetWidth, align)
    }

    private fun truncate(font: Font, run: GlyphRun, scaleX: Float, width: Float, truncate: String) {
        var targetWidth = width
        val glyphCount = run.glyphs.size

        val truncateRun = glyphRunPool.alloc()
        truncateRun.getGlyphsFrom(font, truncate, scaleX, 0, truncate.length, null)
        var truncateWidth = 0f
        if (truncateRun.advances.isNotEmpty()) {
            truncateRun.setLastGlyphAdvanceToWidth(scaleX)
            for (i in 1 until truncateRun.advances.size) {
                truncateWidth += truncateRun.advances[i]
            }
        }
        targetWidth -= truncateWidth

        var count = 0
        var runWidth = run.x
        while (count < run.advances.size) {
            runWidth += run.advances[count]
            if (runWidth >= targetWidth) break
            count++
        }

        if (count > 1) {
            run.glyphs.truncate(count - 1)
            run.advances.size = count
            run.setLastGlyphAdvanceToWidth(scaleX)
            if (truncateRun.advances.isNotEmpty()) {
                run.advances.add(truncateRun.advances.data, 1, truncateRun.advances.size - 1)
            }
        } else {
            run.glyphs.clear()
            run.advances.clear()
            run.advances.add(truncateRun.advances)
        }

        this.glyphCount -= glyphCount - run.glyphs.size

        run.glyphs.addAll(truncateRun.glyphs)
        this.glyphCount += truncate.length
        glyphRunPool.free(truncateRun)
    }

    fun reset() {
        glyphRunPool.free(_runs)
        _runs.clear()
        _colors.clear()
        glyphCount = 0
        width = 0f
        height = 0f
    }

    private fun wrap(font: Font, scale: Float, first: GlyphRun, wrapIndex: Int): GlyphRun? {
        val glyphs2 = first.glyphs
        val glyphCount = first.glyphs.size
        val advances2 = first.advances

        // skip whitespace before the wrap index
        var firstEnd = wrapIndex
        while (firstEnd > 0) {
            if (!font.isWhitespace(glyphs2[firstEnd - 1].code.toChar())) {
                break
            }
            firstEnd--
        }

        // skip whitespace after the wrap index
        var secondStart = wrapIndex
        while (secondStart < glyphCount) {
            if (!font.isWhitespace(glyphs2[secondStart].code.toChar())) {
                break
            }
            secondStart++
        }

        // copy wrapped glyphs and advances to second run
        // the second run will contain the remaining glyph data, so swap instances rather than copying
        var second: GlyphRun? = null
        if (secondStart < glyphCount) {
            val newRun = glyphRunPool.alloc()
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
            advances2.removeAt(0, secondStart)
            first.advances = advances1
            newRun.advances = advances2
            second = newRun
        }

        if (firstEnd == 0) {
            glyphRunPool.free(first)
            _runs.removeLast()
        } else {
            first.setLastGlyphAdvanceToWidth(scale)
        }
        return second
    }

    private fun calculateWidths(scaleX: Float) {
        var width = 0f
        _runs.forEach { run ->
            var runWidth = run.x + run.advances.first()
            var max = 0f
            run.glyphs.forEachIndexed { index, glyph ->
                max = max(max, runWidth + glyph.width * scaleX)
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
            _runs.forEach { run ->
                run.x += if (center) 0.5f * (targetWidth - run.width) else targetWidth - run.width
            }
        }
    }

    private fun GlyphRun.setLastGlyphAdvanceToWidth(scale: Float) {
        advances[advances.size - 1] = glyphs.last().width * scale
    }

}

private fun Font.wrapIndex(glyphs: List<GlyphMetrics>, start: Int): Int {
    var i = start - 1
    var ch = glyphs[i].code.toChar()
    if (isWhitespace(ch)) return i
    if (isWrapChar(ch)) i--
    while (i > 0) {
        ch = glyphs[i].code.toChar()
        if (isWhitespace(ch) || isWrapChar(ch)) return i + 1
        i--
    }
    return 0
}

class GlyphRun {
    var glyphs = arrayListOf<GlyphMetrics>()
    var advances = FloatArrayList()
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var color: Color = Color.WHITE

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

    fun getGlyphsFrom(font: Font, text: CharSequence, scaleX: Float, start: Int, end: Int, lastGlyph: GlyphMetrics?) {
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
            val glyph = font[ch] ?: continue
            //   if (glyph == null) {
            //     glyph = font.missingGlyph ?: continue TODO
            //   }
            glyphs += glyph
            advances += if (currGlyph == null) glyph.xAdvance * scaleX else glyph.xAdvance * scaleX + font.getKerningAmount(
                scaleX,
                currGlyph.code,
                ch.code
            )
            currGlyph = glyph
        } while (i < end)

        if (currGlyph != null) {
            advances += (currGlyph.width - currGlyph.left) * scaleX
        }
    }


    override fun toString(): String {
        return buildString {
            append("\"")
            glyphs.forEach { append(it.code.toChar()) }
            append("\"")
            append(", $x, $y, $width")
        }
    }
}