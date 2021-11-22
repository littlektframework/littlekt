package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
actual class PlatformApplication actual constructor(actual override val configuration: ApplicationConfiguration) :
    Application {

    val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    actual override val graphics: Graphics = WebGLGraphics(canvas)
    actual override val input: Input
        get() = TODO("Not yet implemented")
    actual override val logger: Logger
        get() = TODO("Not yet implemented")
    actual override val assetManager: AssetManager
        get() = TODO("Not yet implemented")
    actual override val fileHandler: FileHandler
        get() = TODO("Not yet implemented")
    actual override val platform: Platform = Platform.JS

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    actual override fun destroy() {
        TODO("Not yet implemented")
    }

}