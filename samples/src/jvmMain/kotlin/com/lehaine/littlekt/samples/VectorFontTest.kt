package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 10/21/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 1100
        height = 700
        vSync = true
        title = "JVM - GPU Font Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        VectorFontTest(it)
    }
}