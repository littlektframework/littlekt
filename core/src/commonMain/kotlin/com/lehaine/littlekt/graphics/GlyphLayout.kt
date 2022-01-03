package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.font.Font
import com.lehaine.littlekt.graphics.font.Glyph
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class GlyphLayout {
    val runs = mutableListOf<GlyphRun>()

    fun setText(
        font: Font, text: String, width: Float = 0f, align: HAlign = HAlign.LEFT, wrap: Boolean = false,
        truncate: String? = null
    ) {
        if (text.isEmpty()) return

        val targetWidth = if (wrap) max(width, font.spaceWidth * 3) else width
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

                val run = GlyphRun().apply {
                    this.x = 0f
                    this.y = y
                    getGlyphsFrom(font, text, runStart, runEnd, null)
                } // TODO alloc and free from a pool instead
                if (currentRun == null) {
                    currentRun = run.also { runs += it }
                } else {
                    currentRun?.append(run)
                }

                if (wrapOrTruncate && (newLine || lastRun)) {
                    currentRun?.let { currentRun ->
                        var runWidth = currentRun.xAdvances.first() + currentRun.xAdvances[1]
                        var i = 1
                        while (i < currentRun.xAdvances.size) {
                            i++
                            val glyph = currentRun.glyphs[i - 1]
                            if (runWidth + glyph.width <= targetWidth) {
                                runWidth += currentRun.xAdvances[i]
                                continue
                            }

                            if (truncate != null) {
                                // TODO truncate
                                return@outer
                            }

                            // TODO handle wrap

                            runs += currentRun
                            y += font.down
                            currentRun.x = 0f
                            currentRun.y = y

                            // start the wrap loop again, another wrap might be necessary
                            runWidth = currentRun.xAdvances.first() + currentRun.xAdvances[1]
                            i = 1
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
    }
}

class GlyphRun {
    val glyphs = arrayListOf<Glyph>()
    val xAdvances = FloatArrayList()
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f

    fun append(run: GlyphRun) {
        glyphs.addAll(run.glyphs)
        if (xAdvances.isNotEmpty()) {
            xAdvances.size--
        }
        xAdvances.add(run.xAdvances)
    }

    fun reset() {
        glyphs.clear()
        xAdvances.clear()
    }

    fun getGlyphsFrom(font: Font, text: CharSequence, start: Int, end: Int, lastGlyph: Glyph?) {
        val max = end - start
        if (max == 0) return

        var i = start
        var currGlyph = lastGlyph
        glyphs.ensureCapacity(max)
        xAdvances.size = max + 1

        do {
            val ch = text[i++]
            if (ch == '\r') continue
            var glyph = font[ch]
            if (glyph == null) {
                glyph = font.missingGlyph ?: continue
            }
            glyphs += glyph
            xAdvances += glyph.advanceWidth
            currGlyph = glyph
        } while (i < end)

        if (currGlyph != null) {
            xAdvances += currGlyph.advanceWidth
        }
    }

    override fun toString(): String {
        return buildString {
            glyphs.forEach { append(it.unicode.toChar()) }
            append(", $x, $y, $width")
        }
    }
}