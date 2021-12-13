package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/9/2021
 */
class GPUFontTest(context: Context) : ContextListener(context) {
    private var loading = true
    private val batch = SpriteBatch(this)
    private val camera = OrthographicCamera().apply {
        left = 0f
        bottom = 0f
        right = graphics.width.toFloat()
        top = graphics.height.toFloat()
    }
    private lateinit var freeSerif: TtfFont
    private lateinit var libSans: TtfFont
    private lateinit var gpuFont: GpuFont

    private var init = false

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
        fileHandler.launch {
            freeSerif = loadTtfFont("FreeSerif.ttf")
            libSans = loadTtfFont("LiberationSans-Regular.ttf")
            loading = false
        }
    }

    private fun init() {
        gpuFont = GpuFont(libSans).also { it.prepare(this@GPUFontTest) }
        gpuFont.insertText("hey there!!", 150f, 250f, 72)
    }

    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !init) {
            init()
            init = true
        }
        camera.update()
        gpuFont.render(camera.viewProjection)

        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { stats.toString() }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }
}