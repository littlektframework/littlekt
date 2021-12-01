package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.Parser
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class HeadParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Head {
        val p = Parser(buffer, start)
        return Head(
            version = p.parseVersion(),
            fontRevision = (p.parseFloat32 * 1000).roundToInt() / 1000,
            checkSumAdjustment = p.parseUint32,
            magicNumber = p.parseUint32.also { check(it == 0x5F0F3CF5) { "Font header has wrong magic number." } },
            flags = p.parseUint16.toInt(),
            unitsPerEm = p.parseUint16.toInt(),
            created = p.parseLongDateTime,
            modified = p.parseLongDateTime,
            xMin = p.parseInt16.toInt(),
            yMin = p.parseInt16.toInt(),
            xMax = p.parseInt16.toInt(),
            yMax = p.parseInt16.toInt(),
            macStyle = p.parseUint16.toInt(),
            lowestRecPPEM = p.parseUint16.toInt(),
            fontDirectionHint = p.parseInt16.toInt(),
            indexToLocFormat = p.parseInt16.toInt(),
            glyphDateFormat = p.parseInt16.toInt()
        )
    }
}

internal class Head(
    val version: Float,
    val fontRevision: Int,
    val checkSumAdjustment: Int,
    val magicNumber: Int,
    val flags: Int,
    val unitsPerEm: Int,
    val created: Int,
    val modified: Int,
    val xMin: Int,
    val yMin: Int,
    val xMax: Int,
    val yMax: Int,
    val macStyle: Int,
    val lowestRecPPEM: Int,
    val fontDirectionHint: Int,
    val indexToLocFormat: Int,
    val glyphDateFormat: Int
)