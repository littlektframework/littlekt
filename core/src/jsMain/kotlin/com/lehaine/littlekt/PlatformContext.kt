package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.file.FileHandler
import com.lehaine.littlekt.file.WebFileHandler
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
actual class PlatformContext actual constructor(actual override val configuration: ApplicationConfiguration) :
    Application {

    val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    actual override val stats: AppStats = AppStats()
    actual override val graphics: Graphics = WebGLGraphics(canvas, stats.engineStats)
    actual override val input: Input = JsInput(canvas)
    actual override val logger: Logger = Logger(configuration.title)
    actual override val fileHandler: FileHandler =
        WebFileHandler(this, logger, configuration.rootPath)
    actual override val platform: Platform = Platform.JS

    private lateinit var game: LittleKt
    private var lastFrame = 0.0
    private var closed = false

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        graphics as WebGLGraphics
        input as JsInput

        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        game = gameBuilder(this)

        Texture.DEFAULT.prepare(game.application)
        window.requestAnimationFrame(::render)
    }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalTime::class)
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
        stats.engineStats.resetPerFrameCounts()
        input as JsInput
        val dt = ((now - lastFrame) / 1000.0).seconds
        lastFrame = now

        input.update()
        stats.update(dt)
        game.render(dt)
        input.reset()

        if (closed) {
            destroy()
        } else {
            window.requestAnimationFrame(::render)
        }
    }

    actual override fun close() {
        closed = true
    }

    actual override fun destroy() {
        game.dispose()
    }

}