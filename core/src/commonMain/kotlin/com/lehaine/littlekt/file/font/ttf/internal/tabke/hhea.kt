package com.lehaine.littlekt.file.font.ttf.internal.tabke

import com.lehaine.littlekt.file.MixedBuffer

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class HheaParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Hhea {
        TODO()
    }
}


internal class Hhea {
    var ascender: Int = 0
    var descender: Int = 0
    var numberOfHMetrics: Int = 0
}