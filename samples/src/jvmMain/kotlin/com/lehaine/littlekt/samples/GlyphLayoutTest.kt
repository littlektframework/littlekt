package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp

/**
 * @author Colton Daily
 * @date 1/3/2021
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Glyph Layout Test"
    }.start {
        GlyphLayoutTest(it)
    }
}