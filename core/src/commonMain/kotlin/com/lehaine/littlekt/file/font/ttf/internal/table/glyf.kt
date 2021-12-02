package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFont
import com.lehaine.littlekt.file.font.ttf.internal.*

/**
 * The `glyf` table describes the glyphs in TrueType outline format.
 * http://www.microsoft.com/typography/otspec/glyf.htm
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class GlyfParser(
    val buffer: MixedBuffer,
    val start: Int,
    val loca: IntArray,
    val font: TtfFont
) {

    fun parse(): GlyphSet {
        val glyphs = GlyphSet(font)
        for (i in 0 until loca.size - 1) {
            val offset = loca[i]
            val nextOffset = loca[i + 1]

            if (offset != nextOffset) {
                glyphs[i] = TTfGlyphLoader(font, i, ::parseGlyph, buffer, start + offset, ::buildPath)
            } else {
                glyphs[i] = SimpleGlyphLoader(font, i)
            }
        }
        return glyphs
    }

    private fun parseGlyph(glyph: Glyph, buffer: MixedBuffer, start: Int) {
        val p = Parser(buffer, start)
        glyph.numberOfContours = p.parseInt16.toInt()
        glyph.xMin = p.parseInt16.toInt()
        glyph.yMin = p.parseInt16.toInt()
        glyph.xMax = p.parseInt16.toInt()
        glyph.yMax = p.parseInt16.toInt()


        if (glyph.numberOfContours > 0) {
            val flags = mutableListOf<Int>()
            var flag: Int
            glyph.endPointIndices.clear()
            glyph.instructions.clear()

            for (i in 0 until glyph.numberOfContours) {
                glyph.endPointIndices += p.parseUint16.toInt()
            }
            glyph.instructionLength = p.parseUint16.toInt()
            for (i in 0 until glyph.instructionLength) {
                glyph.instructions += p.parseByte
            }

            val numOfCoordinates = glyph.endPointIndices[glyph.endPointIndices.size - 1] + 1
            var idx = 0
            while (idx in 0 until numOfCoordinates) {
                flag = p.parseByte.toInt()
                flags += flag

                if ((flag and 8) > 0) {
                    val repeatCount = p.parseByte.toInt()
                    for (j in 0 until repeatCount) {
                        flags += flag
                        idx += 1
                    }
                }
                idx++
            }
            check(flags.size == numOfCoordinates) { "Bad flags." }

            if (glyph.endPointIndices.isNotEmpty()) {
                if (numOfCoordinates > 0) {
                    for (i in 0 until numOfCoordinates) {
                        flag = flags[i]
                        glyph.points += Point(
                            onCurve = (flag and 1) != 0,
                            lastPointOfContour = glyph.endPointIndices.indexOf(i) >= 0
                        )
                    }

                    var px = 0
                    for (i in 0 until numOfCoordinates) {
                        flag = flags[i]
                        glyph.points[i].apply {
                            x = parseGlyphCoord(p, flag, px, 2, 16)
                        }.also { px = it.x }
                    }

                    var py = 0
                    for (i in 0 until numOfCoordinates) {
                        flag = flags[i]
                        glyph.points[i].apply {
                            y = parseGlyphCoord(p, flag, py, 4, 32)
                        }.also { py = it.y }
                    }
                }
            }
        } else if (glyph.numberOfContours == 0) {
            glyph.points.clear()
        } else {
            glyph.isComposite = true
            glyph.points.clear()
            glyph.refs.clear()
            var moreRefs = true
            var flags = 0
            while (moreRefs) {
                flags = p.parseUint16.toInt()
                val ref = GlyphReference(p.parseUint16.toInt(), 0, 0, 1f, 0f, 0f, 1f)
                if ((flags and 1) > 0) {
                    if ((flags and 2) > 0) {
                        ref.x = p.parseUint16.toInt()
                        ref.y = p.parseUint16.toInt()
                    } else {
                        ref.matchedPoints.apply {
                            this[0] = p.parseUint16.toInt()
                            this[1] = p.parseUint16.toInt()
                        }
                    }
                } else {
                    if ((flags and 2) > 0) {
                        ref.x = p.parseChar.code
                        ref.y = p.parseChar.code
                    } else {
                        ref.matchedPoints.apply {
                            this[0] = p.parseByte.toInt()
                            this[1] = p.parseByte.toInt()
                        }
                    }
                }
                when {
                    (flags and 8) > 0 -> {
                        ref.scaleX = p.parseF2Dot14.toFloat()
                        ref.scaleY = ref.scaleX
                    }
                    (flags and 64) > 0 -> {
                        ref.scaleX = p.parseF2Dot14.toFloat()
                        ref.scaleY = p.parseF2Dot14.toFloat()
                    }
                    (flags and 128) > 0 -> {
                        ref.scaleX = p.parseF2Dot14.toFloat()
                        ref.scale01 = p.parseF2Dot14.toFloat()
                        ref.scale10 = p.parseF2Dot14.toFloat()
                        ref.scaleY = p.parseF2Dot14.toFloat()
                    }
                }
                glyph.refs += ref
                moreRefs = (flags and 32) != 0
            }
            if (flags and 0x100 != 0) {
                glyph.instructionLength = p.parseUint16.toInt()
                glyph.instructions.clear()
                for (i in 0 until glyph.instructionLength) {
                    glyph.instructions += p.parseByte
                }
            }
        }
    }

    private fun parseGlyphCoord(p: Parser, flag: Int, prevValue: Int, shortVectorBitMask: Int, sameBitMask: Int): Int {
        var v: Int
        if ((flag and shortVectorBitMask) > 0) {
            v = p.parseByte.toInt()

            if ((flag and sameBitMask) == 0) {
                v = -v
            }
            v += prevValue
        } else {
            v = if ((flag and sameBitMask) > 0) {
                prevValue
            } else {
                prevValue + p.parseInt16
            }
        }
        return v
    }

    fun buildPath(glyphSet: GlyphSet, glyph: Glyph) {
        if (glyph.isComposite) {
            // TODO impl building path for composite glyphs
        }

        calcPath(glyph)
    }

    fun calcPath(glyph: Glyph) {
        if (glyph.points.isEmpty()) return
        val p = Path()
        val contours = getContours(glyph.points)
        contours.forEach { contour ->
            var prev: Point? = null
            var curr = contour[contour.size - 1]
            var next = contour[0]

            if (curr.onCurve) {
                p.moveTo(curr.x.toFloat(), curr.y.toFloat())
            } else {
                if (next.onCurve) {
                    p.moveTo(next.x.toFloat(), next.y.toFloat())
                } else {
                    val startX = (curr.x + next.x) * 0.5f
                    val startY = (curr.y + next.y) * 0.5f
                    p.moveTo(startX, startY)
                }
            }

            for (i in contour.indices) {
                prev = curr
                curr = next
                next = contour[(i + 1) % contour.size]

                if (curr.onCurve) {
                    p.lineTo(curr.x.toFloat(), curr.y.toFloat())
                } else {
                    var next2 = next.x.toFloat() to next.y.toFloat()

                    if (!next.onCurve) {
                        next2 = ((curr.x + next.x) * 0.5f) to ((curr.y + next.y) * 0.5f)
                    }
                    p.quadTo(curr.x.toFloat(), curr.y.toFloat(), next2.first, next2.second)
                }
            }
            p.close()
        }
        glyph.path = p
    }

    fun getContours(points: List<Point>): List<List<Point>> {
        val contours = mutableListOf<List<Point>>()
        var current = mutableListOf<Point>()
        points.forEach {
            current += it
            if (it.lastPointOfContour) {
                contours += current
                current = mutableListOf()
            }
        }
        return contours
    }
}