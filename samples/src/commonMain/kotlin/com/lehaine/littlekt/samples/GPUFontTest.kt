package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readTtfFont
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.geom.degrees
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/9/2021
 */
class GPUFontTest(context: Context) : ContextListener(context) {
    private var loading = true
    private val camera = OrthographicCamera(graphics.width, graphics.height)
    private lateinit var freeSerif: TtfFont
    private lateinit var libSans: TtfFont
    private lateinit var gpuFont: GpuFont
    private var lastStats: String = "TBD"
    private var init = false

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
        vfs.launch {
            freeSerif = resourcesVfs["FreeSerif.ttf"].readTtfFont()
            libSans = resourcesVfs["LiberationSans-Regular.ttf"].readTtfFont()
            loading = false
        }
        camera.translate(graphics.width / 2f, graphics.height / 2f, 0f)
    }

    private fun init() {
        gpuFont = GpuFont(this, freeSerif).apply {
            debug = true
        }
    }

    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !init) {
            init()
            init = true
        }
        gl.clearColor(Color.DARK_GRAY)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        camera.update()
        gpuFont.use(camera.viewProjection) {
            it.drawText("I should hopefully be wrapped text.", 150f, 50f, maxWidth = 250f, pxSize = 36, wrap = true)
            it.drawText(
                "I am rotated!!\ngYnlqQp",
                550f,
                250f,
                36,
                rotation = 45f.degrees,
                color = Color.BLUE
            )
            it.drawText("I am a different font!!!!", 450f, 450f, 44, color = Color.DARK_RED, font = libSans)

            it.drawText(
                "This is center aligned text which is pretty cool",
                150f,
                200f,
                36,
                maxWidth = 250f,
                align = HAlign.CENTER,
                color = Color.DARK_YELLOW,
                wrap = true
            )

            it.drawText(
                "This is right aligned text which is also cool",
                150f,
                350f,
                36,
                maxWidth = 250f,
                align = HAlign.RIGHT,
                color = Color.DARK_CYAN,
                wrap = true
            )
        }
        lastStats = stats.toString()
        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { stats.toString() }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }
}