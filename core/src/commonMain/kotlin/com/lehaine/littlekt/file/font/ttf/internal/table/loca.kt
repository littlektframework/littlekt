package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.Parser

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class LocaParser(val mixedBuffer: MixedBuffer, val start: Int, val numGlyphs: Int, val shortVersion: Boolean) {


    fun parse(): IntArray {
        val p = Parser(mixedBuffer, start)
        val parseFn = if (shortVersion) p::parseUint16 else p::parseUint32

        val glyphOffsets = intArrayOf(numGlyphs + 1)
        for (i in 0 until numGlyphs + 1) {
            var glyphOffset = parseFn.get().toInt()
            if (shortVersion) {
                glyphOffset *= 2
            }
            glyphOffsets[i] = glyphOffset
        }
        return glyphOffsets
    }
}