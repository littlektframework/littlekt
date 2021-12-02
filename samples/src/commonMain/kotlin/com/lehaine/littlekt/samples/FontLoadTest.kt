package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
class FontLoadTest(application: Application) : LittleKt(application) {
    init {
        fileHandler.launch {
            val font = loadTtfFont("FreeSerif.ttf")
            val glyph = font.charToGlyph('D').also { it.path }
            println(glyph)
            //println(font)
        }
    }
}