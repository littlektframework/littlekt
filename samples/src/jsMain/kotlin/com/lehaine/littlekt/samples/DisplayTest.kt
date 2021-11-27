package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
fun main() {
    createLittleKtApp {
        title = "JS - Display Test"
    }.start {
        DisplayTest(it)
    }
}