package com.lehaine.littlekt

import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class ApplicationConfiguration(
    actual val title: String = "LittleKt - JS",
    val canvasId: String = "canvas",
    val rootPath: String = "./"
) {

}