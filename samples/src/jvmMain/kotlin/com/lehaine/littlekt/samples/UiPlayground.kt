package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 10/26/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 1280
        height = 720
        vSync = true
        title = "JVM - UI Playground"
        backgroundColor = Color.DARK_GRAY
    }.start {
        UiPlayground(it)
    }
}