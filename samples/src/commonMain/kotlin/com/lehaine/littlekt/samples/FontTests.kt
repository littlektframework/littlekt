package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.toString
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/3/2022
 */
class FontTests(context: Context) : Game<Scene>(context) {

    val libSans by load<TtfFont>(resourcesVfs["LiberationSans-Regular.ttf"])
    val pixelFont by load<BitmapFont>(resourcesVfs["m5x7_16.fnt"])
    val pixelFontLarge by load<BitmapFont>(resourcesVfs["m5x7_32.fnt"])
    val pixelFontXLarge by load<BitmapFont>(resourcesVfs["m5x7_48.fnt"])

    val barlowLargeCache by prepare {
        BitmapFontCache(Fonts.large)
    }

    val pixelFontXLargeCache by prepare {
        BitmapFontCache(pixelFontXLarge)
    }
    val batch = SpriteBatch(this)

    val camera = OrthographicCamera(context.graphics.width, context.graphics.height).also {
        it.translate(graphics.width / 2f, graphics.height / 2f, 0f)
    }


    override fun create() {
        barlowLargeCache.setText(
            "aabbccddeeffgghhiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz112233445566778899",
            50f,
            400f,
            color = Color.RED
        )
        pixelFontXLargeCache.setText("This is m5x7 point 48", 550f, 100f, rotation = 45.degrees)
    }

    private var lastFps: Double = 0.0
    private var drawCalls: Int = 0
    override fun update(dt: Duration) {
        gl.clearColor(Color.DARK_GRAY)

        camera.update()
        batch.use(camera.viewProjection) {
            pixelFontLarge.draw(it, "FPS: ${lastFps.toString(1)}", 600f, 25f)
            pixelFontLarge.draw(it, "Draw calls: $drawCalls", 600f, 50f)
            pixelFont.draw(it, "This is m5x7 point 16", 0f, 0f)
            pixelFontLarge.draw(it, "This is m5x7 point 32", 50f, 75f)
            pixelFontXLarge.draw(it, "This is m5x7 point 48", 50f, 100f)
            pixelFontXLargeCache.draw(it)
            barlowLargeCache.draw(it)
            Fonts.tiny.draw(it, "This is barlow point 9", 50f, 250f)
            Fonts.small.draw(it, "This is barlow point 11", 50f, 275f)
            Fonts.default.draw(it, "This is barlow point 17", 50f, 300f)
            Fonts.large.draw(it, "This is barlow point 32", 50f, 325f)
        }

        lastFps = stats.fps
        drawCalls = stats.engineStats.drawCalls
    }

}