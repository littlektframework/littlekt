package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.Parser

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class HheaParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Hhea {
        val p = Parser(buffer, start)
        return Hhea(
            version = p.parseVersion(),
            ascender = p.parseInt16.toInt(),
            descender = p.parseInt16.toInt(),
            lineGap = p.parseInt16.toInt(),
            advanceWidthMax = p.parseUint16.toInt(),
            minLeftSideBearing = p.parseInt16.toInt(),
            minRightSideBearing = p.parseInt16.toInt(),
            xMaxExtent = p.parseInt16.toInt(),
            caretSlopeRise = p.parseInt16.toInt(),
            caretSlopeRun = p.parseInt16.toInt(),
            caretOffset = p.parseInt16.toInt().also { p.relativeOffset += 8 },
            metricDataFormat = p.parseInt16.toInt(),
            numberOfHMetrics = p.parseUint16.toInt(),
        )
    }
}

internal data class Hhea(
    val version: Float,
    val ascender: Int,
    val descender: Int,
    val lineGap: Int,
    val advanceWidthMax: Int,
    val minLeftSideBearing: Int,
    val minRightSideBearing: Int,
    val xMaxExtent: Int,
    val caretSlopeRise: Int,
    val caretSlopeRun: Int,
    val caretOffset: Int,
    val metricDataFormat: Int,
    val numberOfHMetrics: Int
)