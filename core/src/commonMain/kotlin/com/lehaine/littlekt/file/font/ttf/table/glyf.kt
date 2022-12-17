package com.lehaine.littlekt.file.font.ttf.table

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.font.ttf.*
import com.lehaine.littlekt.graphics.g2d.font.GlyphPath
import kotlin.math.floor

/**
 * The `glyf` table describes the glyphs in TrueType outline format.
 * http://www.microsoft.com/typography/otspec/glyf.htm
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class GlyfParser(
    val buffer: ByteBuffer,
    val start: Int,
    val loca: IntArray,
    val fontReader: TtfFontReader
) {

    fun parse(): GlyphSet {
        val glyphs = GlyphSet()

        // The last element of the loca table is invalid.
        for (i in 0 until loca.size - 1) {
            val offset = loca[i]
            val nextOffset = loca[i + 1]

            if (offset != nextOffset) {
                glyphs[i] = TTfGlyphLoader(
                    fontReader = fontReader,
                    index = i,
                    unitsPerEm = fontReader.unitsPerEm,
                    parseGlyph = ::parseGlyph,
                    buffer = buffer,
                    position = start + offset,
                    buildPath = ::buildPath
                )
            } else {
                glyphs[i] = SimpleGlyphLoader(i, fontReader.unitsPerEm)
            }
        }
        return glyphs
    }

    private fun parseGlyph(glyph: MutableGlyph, buffer: ByteBuffer, start: Int) {
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
                glyph.endPointIndices += p.parseUint16
            }
            glyph.instructionLength = p.parseUint16
            for (i in 0 until glyph.instructionLength) {
                glyph.instructions += p.parseByte
            }

            val numOfCoordinates = glyph.endPointIndices[glyph.endPointIndices.size - 1] + 1
            var idx = 0
            while (idx in 0 until numOfCoordinates) {
                flag = p.parseUByte
                flags += flag

                if ((flag and 8) > 0) {
                    val repeatCount = p.parseUByte
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
                        glyph.points += MutablePoint(
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
                flags = p.parseUint16
                val ref = MutableGlyphReference(p.parseUint16, 0, 0, 1f, 0f, 0f, 1f)
                if ((flags and 1) > 0) {
                    if ((flags and 2) > 0) {
                        ref.x = p.parseInt16.toInt()
                        ref.y = p.parseInt16.toInt()
                    } else {
                        ref.matchedPoints = intArrayOf(p.parseUint16, p.parseUint16)
                    }
                } else {
                    if ((flags and 2) > 0) {
                        ref.x = p.parseChar.code
                        ref.y = p.parseChar.code
                    } else {
                        ref.matchedPoints = intArrayOf(p.parseByte.toInt(), p.parseByte.toInt())
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
                glyph.instructionLength = p.parseUint16
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
            val b = p.parseUByte
            v = b

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

    fun buildPath(glyphSet: GlyphSet, glyph: MutableGlyph) {
        if (glyph.isComposite) {
            glyph.refs.forEach { ref ->
                val glyphRef = glyphSet[ref.glyphIndex].also { it.calcPath() }
                if (glyphRef.points.isNotEmpty()) {
                    val transformedPoints: MutableList<MutablePoint>
                    val matchedPoints = ref.matchedPoints
                    if (matchedPoints == null) {
                        // ref positioned by offset
                        transformedPoints = transformPoints(glyphRef.points, ref)
                    } else {
                        // ref positioned by matched points
                        check(matchedPoints[0] <= glyph.points.size - 1 && matchedPoints[1] <= glyphRef.points.size - 1) {
                            "Matched points out of range in ${glyph.name}"
                        }

                        val firstPt = glyph.points[matchedPoints[0]]
                        var secondPt = glyphRef.points[matchedPoints[1]]
                        val transformRef = MutableGlyphReference(
                            glyphIndex = -1,
                            x = 0,
                            y = 0,
                            scaleX = ref.scaleX,
                            scale01 = ref.scale01,
                            scale10 = ref.scale10,
                            scaleY = ref.scaleY
                        )
                        secondPt = transformPoints(listOf(secondPt), transformRef)[0]
                        transformRef.x = firstPt.x - secondPt.x
                        transformRef.y = firstPt.y - secondPt.y
                        transformedPoints = transformPoints(glyphRef.points, transformRef)
                    }
                    glyph.points.addAll(transformedPoints)
                }
                if (glyph.numberOfContours < 0) {
                    glyph.numberOfContours = 0
                }
                glyph.numberOfContours += glyphRef.numberOfContours
            }
        }

        calcPath(glyph)
    }

    fun transformPoints(points: List<MutablePoint>, ref: MutableGlyphReference): MutableList<MutablePoint> {
        val newPoints = mutableListOf<MutablePoint>()
        points.forEach {
            newPoints += MutablePoint(
                x = (ref.scaleX * it.x + ref.scale01 * it.y + ref.x).toInt(),
                y = (ref.scale10 * it.x + ref.scaleY * it.y + ref.y).toInt(),
                onCurve = it.onCurve,
                lastPointOfContour = it.lastPointOfContour
            )
        }
        return newPoints
    }

    fun calcPath(glyph: MutableGlyph) {
        if (glyph.points.isEmpty()) return
        val p = GlyphPath(glyph.unitsPerEm)
        val contours = getContours(glyph.points)
        contours.forEach { contour ->
            var curr = contour[contour.size - 1]
            var next = contour[0]
            var moved = false
            if (curr.onCurve) {
                p.moveTo(curr.x.toFloat(), curr.y.toFloat())
            } else {
                if (next.onCurve) {
                    p.moveTo(next.x.toFloat(), next.y.toFloat())
                    moved = true
                } else {
                    // If both first and last points are off-curve, start at their middle.
                    val startX = floor((curr.x + next.x) * 0.5f)
                    val startY = floor((curr.y + next.y) * 0.5f)
                    p.moveTo(startX, startY)
                }
            }

            for (i in contour.indices) {
                curr = next
                next = contour[(i + 1) % contour.size]

                if (moved) {
                    moved = false
                    continue
                }

                if (curr.onCurve) {
                    // This is a straight line.
                    p.lineTo(curr.x.toFloat(), curr.y.toFloat())
                } else {
                    var next2 = next.x.toFloat() to next.y.toFloat()

                    if (next.onCurve) {
                        moved = true
                    } else {
                        next2 = floor((curr.x + next.x) * 0.5f) to floor((curr.y + next.y) * 0.5f)
                    }
                    p.quadTo(curr.x.toFloat(), curr.y.toFloat(), next2.first, next2.second)

                }
            }
            p.close()
        }
        glyph.path = p
    }

    fun getContours(points: List<MutablePoint>): List<List<MutablePoint>> {
        val contours = mutableListOf<List<MutablePoint>>()
        var current = mutableListOf<MutablePoint>()
        points.forEach {
            current += it
            if (it.lastPointOfContour) {
                contours += current
                current = mutableListOf()
            }
        }
        check(current.isEmpty()) { "There are still points left in the current contour." }
        return contours
    }
}