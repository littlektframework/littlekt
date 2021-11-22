package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
fun main() {
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("JS - Display Test") },
        gameBuilder = { DisplayTest(it) })
        .start()
}