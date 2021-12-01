package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.TtfFont
import com.lehaine.littlekt.file.font.ttf.internal.GlyphSet

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class GlyfParser(
    val buffer: MixedBuffer,
    val start: Int,
    val loca: IntArray,
    val font: TtfFont,
    useLowMemory: Boolean = false
) {


    fun parse(): GlyphSet {
        val glyphs = GlyphSet(font)
        for(i in 0 until loca.size-1) {
            val offset = loca[i]
            val nextOffset = loca[i+1]

            if(offset != nextOffset) {
                glyphs[i] =
            }
        }
        return glyphs
    }
}