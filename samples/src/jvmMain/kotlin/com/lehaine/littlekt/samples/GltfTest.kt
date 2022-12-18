package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.samples.s3d.GltfTest

/**
 * @author Colton Daily
 * @date 12/18/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - glTF Test"
        backgroundColor = Color.DARK_GRAY
    }.start {
        GltfTest(it)
    }
}