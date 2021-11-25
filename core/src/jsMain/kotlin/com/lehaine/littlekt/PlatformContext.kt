package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.io.WebFileHandler
import com.lehaine.littlekt.log.JsLogger
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
actual class PlatformContext actual constructor(actual override val configuration: ApplicationConfiguration) :
    Application {

    val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    actual override val engineStats: EngineStats = EngineStats()
    actual override val graphics: Graphics = WebGLGraphics(canvas, engineStats)
    actual override val input: Input = JsInput(canvas)
    actual override val logger: Logger = JsLogger(configuration.title)
    actual override val fileHandler: FileHandler =
        WebFileHandler(this, logger, configuration.rootPath)
    actual override val platform: Platform = Platform.JS

    private lateinit var game: LittleKt
    private var lastFrame = 0.0

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        graphics as WebGLGraphics
        input as JsInput

        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        game = gameBuilder(this)

        Texture.DEFAULT.prepare(game.application)
        window.requestAnimationFrame(::render)
    }

    private fun render(now: Double) {
        if (canvas.clientWidth != graphics.width ||
            canvas.clientHeight != graphics.height
        ) {
            graphics as WebGLGraphics
            graphics._width = canvas.clientWidth
            graphics._height = canvas.clientHeight
            canvas.width = canvas.clientWidth
            canvas.height = canvas.clientHeight
            game.resize(graphics.width, graphics.height)
        }
        engineStats.resetPerFrameCounts()
        input as JsInput
        val dt = (now - lastFrame) / 1000.0
        lastFrame = now
        input.update()
        game.render(dt.toFloat())
        input.reset()

        window.requestAnimationFrame(::render)
    }

    actual override fun close() {
        // nothing to do - we don't want to close the browser window.
    }

    actual override fun destroy() {
        // nothing to do
    }

}