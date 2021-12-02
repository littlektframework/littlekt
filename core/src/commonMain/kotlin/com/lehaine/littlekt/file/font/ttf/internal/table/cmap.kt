package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.Parser
import com.lehaine.littlekt.file.font.ttf.internal.Type

/**
 * The `cmap` table stores the mappings from characters to glyphs.
 * https://www.microsoft.com/typography/OTSPEC/cmap.htm
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class CmapParser(private val buffer: MixedBuffer, private val start: Int) {

    fun parse(): Cmap {
        val cmap = MutableCmap()
        cmap.version = buffer.getUint16(start).toInt()
        check(cmap.version == 0) { "cmap table version should be 0!" }

        cmap.numTables = buffer.getUint16(start + 2).toInt()
        var offset = -1
        for (i in cmap.numTables - 1 downTo 0) {
            val platformId = buffer.getUint16(start + 4 + (i * 8)).toInt()
            val encodingId = buffer.getUint16(start + 4 + (i * 8) + 2).toInt()
            if ((platformId == 3 && (encodingId == 0 || encodingId == 1 || encodingId == 10)) ||
                (platformId == 0 && (encodingId == 0 || encodingId == 1 || encodingId == 2 || encodingId == 3 || encodingId == 4))
            ) {
                offset = buffer.getUint32(start + 4 + (i * 8) + 4)
                break
            }
        }
        if (offset == -1) {
            throw IllegalStateException("No valid cmap sub-tables found")
        }
        val p = Parser(buffer, start + offset)
        cmap.format = p.parseUint16.toInt()
        when (cmap.format) {
            12 -> {
                parseFormat12(cmap, p)
            }
            4 -> {
                parseFormat4(cmap, p, start, offset)
            }
            else -> {
                throw IllegalStateException("Only format 4 and 12 cmap tables are supported (found format ${cmap.format}).")
            }
        }

        return cmap.toCmap()
    }

    private fun parseFormat4(cmap: MutableCmap, p: Parser, start: Int, offset: Int) {
        cmap.length = p.parseUint16.toInt()
        cmap.language = p.parseUint16.toInt()
        val segCount = p.parseUint16.toInt() shr 1
        cmap.segCount = segCount
        p.skip(Type.SHORT, 3)

        val endCountParser = Parser(buffer, start + offset + 14)
        val startCountParser = Parser(buffer, start + offset + 16 + segCount * 2)
        val idDeltaParser = Parser(buffer, start + offset + 16 + segCount * 4)
        val idRangeOffsetParser = Parser(buffer, start + offset + 16 + segCount * 6)
        var glyphIndexOffset: Int
        for (i in 0 until segCount - 1) {
            var glyphIndex: Int
            val endCount = endCountParser.parseUint16.toInt()
            val startCount = startCountParser.parseUint16.toInt()
            val idDelta = idDeltaParser.parseInt16.toInt()
            val idRangeOffset = idRangeOffsetParser.parseUint16.toInt()
            for (c in startCount..endCount) {
                if (idRangeOffset != 0) {
                    glyphIndexOffset = (idRangeOffsetParser.offset + idRangeOffsetParser.relativeOffset - 2)
                    glyphIndexOffset += idRangeOffset
                    glyphIndexOffset += (c - startCount) * 2
                    glyphIndex = buffer.getUint16(glyphIndexOffset).toInt()
                    if (glyphIndex != 0) {
                        glyphIndex = (glyphIndex + idDelta) and 0xFFFF
                    }
                } else {
                    glyphIndex = (c + idDelta) and 0xFFFF
                }

                cmap.glyphIndexMap[c] = glyphIndex
            }
        }
    }

    private fun parseFormat12(cmap: MutableCmap, p: Parser) {
        p.parseUint16

        cmap.length = p.parseUint32
        cmap.language = p.parseUint32

        val groupCount = p.parseUint32
        cmap.groupCount = groupCount

        for (i in 0 until groupCount) {
            val startCharCode = p.parseUint32
            val endCharCode = p.parseUint32
            var startGlyphId = p.parseUint32

            for (c in startCharCode..endCharCode) {
                cmap.glyphIndexMap[c] = startGlyphId
                startGlyphId++
            }
        }
    }
}

private class MutableCmap {

    var version: Int = 0
    var numTables: Int = 0
    var format: Int = 0
    var length: Int = 0
    var language: Int = 0
    var groupCount: Int = 0
    val glyphIndexMap: MutableMap<Int, Int> = mutableMapOf()
    var segCount: Int = 0

    fun toCmap() = Cmap(version, numTables, format, length, language, groupCount, glyphIndexMap, segCount)
}

/**
 * The `cmap` table stores the mappings from characters to glyphs.
 * https://www.microsoft.com/typography/OTSPEC/cmap.htm
 * @author Colton Daily
 * @date 11/30/2021
 */
internal data class Cmap(
    val version: Int,
    val numTables: Int,
    val format: Int,
    val length: Int,
    val language: Int,
    val groupCount: Int,
    val glyphIndexMap: Map<Int, Int>,
    val segCount: Int
) {
    override fun toString(): String {
        return "Cmap(version=$version, numTables=$numTables, format=$format, length=$length, language=$language, groupCount=$groupCount, segCount=$segCount)"
    }
}