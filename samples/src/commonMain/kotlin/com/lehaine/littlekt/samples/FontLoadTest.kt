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
        logger.level = Logger.Level.DEBUG
        fileHandler.launch {
            val font = loadTtfFont("FreeSerif.ttf")
          //  font.fontSize = 1
            gpuFont = GPUFont(font)
            val glyph = font.glyphs['D'.code]
            println(glyph?.points)
            loading = false
        }
    }

    var text = "ABCDEF"
    var text2 = "hello!"
    var usingText = true
    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !gpuFont.prepared) {
            gpuFont.prepare(this)
        }
        camera.update()
        gl.clearColor(Color.DARK_GRAY)

        gpuFont.text(if (usingText) text else text2, 450f, 230f)
        gpuFont.flush(batch, camera.viewProjection)

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