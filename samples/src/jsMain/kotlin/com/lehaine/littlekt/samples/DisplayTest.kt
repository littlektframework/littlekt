package com.lehaine.littlekt.samples

import com.lehaine.littlekt.ApplicationConfiguration
import com.lehaine.littlekt.LittleKtAppBuilder
import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
fun main() {
    console.log("main")
    val rootPath =
        (window.location.protocol + "//" + window.location.host + window.location.pathname).replace("index.html", "")
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("JS - Display Test", rootPath = rootPath) },
        gameBuilder = { DisplayTest(it) })
        .start()
}