package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp


fun main() {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Shader Test"
    }.start {
        ShaderTest(it)
    }
}