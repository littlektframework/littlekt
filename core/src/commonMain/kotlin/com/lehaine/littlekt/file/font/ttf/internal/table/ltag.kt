package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.internal.Parser
import com.lehaine.littlekt.file.font.ttf.internal.Type

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class LtagParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): List<String> {
        val p = Parser(buffer, start)
        val tableVersion = p.parseUint32
        check(tableVersion == 1) { "Unsupported table version" }
        p.skip(Type.INT)
        val numTags = p.parseUint32

        val tags = mutableListOf<String>()
        for (i in 0 until numTags) {
            var tag = ""
            val offset = start + p.parseUint16
            val length = p.parseUint16.toInt()
            for (j in offset + 1 until offset + length) {
                tag += buffer.getInt8(j).toInt().toChar()
            }
            tags += tag
        }
        return tags.toList()
    }
}