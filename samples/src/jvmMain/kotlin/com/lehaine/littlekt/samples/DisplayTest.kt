package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import kotlinx.coroutines.runBlocking

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
fun main(args: Array<String>) {
    runBlocking {
        createLittleKtApp {
            width = 960
            height = 540
            vSync = true
            title = "JVM - Display Test"
        }.start {
            DisplayTest(it)
        }
    }
}