package com.lehaine.littlekt

import com.lehaine.littlekt.audio.AudioContext
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.io.WebFileHandler
import com.lehaine.littlekt.log.JsLogger
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
actual class PlatformApplication actual constructor(actual override val configuration: ApplicationConfiguration) :
    Application {

    val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    actual override val graphics: Graphics = WebGLGraphics(canvas)
    actual override val input: Input = JsInput(canvas)
    actual override val logger: Logger = JsLogger(configuration.title)
    actual override val assetManager: AssetManager = AssetManager(this)
    actual override val fileHandler: FileHandler =
        WebFileHandler(this, logger, configuration.rootPath, AudioContext())
    actual override val platform: Platform = Platform.JS

    private lateinit var game: LittleKt
    private var lastFrame = 0.0

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        console.log("Starting")
        graphics as WebGLGraphics
        input as JsInput

        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        game = gameBuilder(this)

        window.requestAnimationFrame(::load)
    }

    private fun load(now: Double) {
        if (fileHandler.isFullyLoaded()) {
            logger.info("JS App") { "Loaded!" }
            game.create()
            game.resize(graphics.width, graphics.height)
            window.requestAnimationFrame(::render)
        } else {
            logger.info("JS App") { "Loading..." }
            assetManager.update()
            window.requestAnimationFrame(::load)
        }
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
        input as JsInput
        val nowInSeconds = now * 0.001
        val delta = nowInSeconds - lastFrame
        lastFrame = nowInSeconds
        val dt = min(1 / 60f, delta.toFloat())
        input.update()
        game.render(dt)
        input.reset()

        if (fileHandler.isFullyLoaded()) {
            window.requestAnimationFrame(::render)
        } else {
            // new resources requested - load them before continuing
            window.requestAnimationFrame(::load)
        }
    }

    actual override fun close() {
        // nothing to do - don't want to close the browser
    }

    actual override fun destroy() {
        // TODO - anything we need to clean up?
    }

}