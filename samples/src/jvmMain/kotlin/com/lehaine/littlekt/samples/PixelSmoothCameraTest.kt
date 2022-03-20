package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 544
        vSync = true
        title = "JVM - Pixel Smooth Camera Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        PixelSmoothCameraTest(it)
    }
}