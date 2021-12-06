package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GPUFont
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
class FontLoadTest(application: Application) : LittleKt(application) {
    lateinit var gpuFont: GPUFont
    private var loading = true
    private val batch = SpriteBatch(this)
    private val camera = OrthographicCamera().apply {
        left = 0f
        right = graphics.width.toFloat()
        bottom = 0f
        top = graphics.height.toFloat()
    }

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
        fileHandler.launch {
            val font = loadTtfFont("FreeSerif.ttf")
            gpuFont = GPUFont(font)
            loading = false
        }
    }

    var text = "Hello World!!!"
    var text2 = "Aa Bb Cc Dd Ee Ff Gg @#4@*@*#&()_!@#"
    var usingText = true
    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !gpuFont.prepared) {
            gpuFont.prepare(this)
        }
        camera.update()
        gl.clearColor(Color.DARK_GRAY)

        gpuFont.fontSize = 36
        gpuFont.text(if (usingText) text else text2, 50f, 430f, Color.DARK_ORANGE)
        gpuFont.flush(camera.viewProjection)

        gpuFont.fontSize = 100
        gpuFont.buildText(50f, 330f) {
            append(Color.WHITE) { "This" }
            append(Color.GREEN) { " is" }
            append(Color.BLUE) { " awesome!" }
            append(Color.RED) { "!" }
            append(Color.YELLOW) { "!" }
        }
        gpuFont.flush(camera.viewProjection)

        gpuFont.fontSize = 72
        gpuFont.text("Offscreen, with jitter", 50f, 230f)
        gpuFont.flush(batch, camera.viewProjection, Color.GREEN)

        gpuFont.fontSize = 48
        gpuFont.text("Offscreen, no jitter", 50f, 130f)
        gpuFont.flush(batch, camera.viewProjection, useJitter = false)

        gl.clearColor(Color.DARK_GRAY)

        if (input.isKeyJustPressed(Key.ENTER)) {
            usingText = !usingText
        }

        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { stats }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }
}