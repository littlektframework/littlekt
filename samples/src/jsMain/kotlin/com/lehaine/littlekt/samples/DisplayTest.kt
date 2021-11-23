package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
fun main() {
    val rootPath =
        (window.location.protocol + "//" + window.location.host + window.location.pathname).replace("index.html", "")
    createLittleKtApp {
        title = "JVM - Display Test"
        this.rootPath = rootPath
        { DisplayTest(it) }
    }.start()
}