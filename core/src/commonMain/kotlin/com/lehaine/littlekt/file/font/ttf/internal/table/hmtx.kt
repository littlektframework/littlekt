package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.GlyphSet
import com.lehaine.littlekt.file.font.ttf.internal.Parser

/**
 * The `hmtx` table contains the horizontal metrics for all glyphs.
 * https://www.microsoft.com/typography/OTSPEC/hmtx.htm
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class HmtxParser(
    val buffer: MixedBuffer,
    val start: Int,
    val numOfHMetrics: Int,
    val numGlyphs: Int,
    val glyphs: GlyphSet
) {
    fun parse() {
        var advanceWidth = 0
        var leftSideBearing = 0
        val p = Parser(buffer, start)

        for (i in 0 until numGlyphs) {
            if (i < numOfHMetrics) {
                advanceWidth = p.parseUint16.toInt()
                leftSideBearing = p.parseInt16.toInt()
            }
            glyphs[i].apply {
                this.advanceWidth = advanceWidth.toFloat()
                this.leftSideBearing = leftSideBearing
            }
        }
    }
}