package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 8/1/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Viewports Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        ViewportsTest(it)
    }
}