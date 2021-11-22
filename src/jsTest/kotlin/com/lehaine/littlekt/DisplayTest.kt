package com.lehaine.littlekt

import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
fun main() {
    val rootPath =
        (window.location.protocol + "//" + window.location.host + window.location.pathname).replace("index.html", "")
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("JS - Display Test", rootPath = rootPath) },
        gameBuilder = { DisplayTest(it) })
        .start()
}