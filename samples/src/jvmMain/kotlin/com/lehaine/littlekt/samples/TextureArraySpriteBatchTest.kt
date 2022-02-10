package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Texture Array Sprite Batch Test"
    }.start {
        TextureArraySpriteBatchTest(it)
    }
}