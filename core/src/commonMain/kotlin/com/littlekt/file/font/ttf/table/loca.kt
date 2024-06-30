package com.littlekt.file.font.ttf.table

import com.littlekt.file.ByteBuffer
import com.littlekt.file.font.ttf.Parser

/**
 * The `loca` table stores the offsets to the locations of the glyphs in the font.
 * https://www.microsoft.com/typography/OTSPEC/loca.htm
 *
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class LocaParser(
    val byteBuffer: ByteBuffer,
    val start: Int,
    val numGlyphs: Int,
    val shortVersion: Boolean
) {

    fun parse(): IntArray {
        val p = Parser(byteBuffer, start)
        val parseFn = if (shortVersion) p::parseUint16 else p::parseInt32

        val glyphOffsets = IntArray(numGlyphs + 1)
        for (i in 0 until numGlyphs) {
            var glyphOffset = parseFn.get()
            if (shortVersion) {
                glyphOffset *= 2
            }
            glyphOffsets[i] = glyphOffset
        }
        return glyphOffsets
    }
}
