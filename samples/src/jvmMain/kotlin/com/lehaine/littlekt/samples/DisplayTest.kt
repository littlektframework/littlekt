package com.lehaine.littlekt.samples

import com.lehaine.littlekt.ApplicationConfiguration
import com.lehaine.littlekt.LittleKtAppBuilder

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
fun main(args: Array<String>) {
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("JVM - Display Test", 960, 540, true) },
        gameBuilder = { DisplayTest(it) })
        .start()
}