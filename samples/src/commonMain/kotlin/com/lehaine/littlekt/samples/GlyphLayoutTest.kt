package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.GlyphLayout
import com.lehaine.littlekt.graphics.font.TtfFont
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/3/2022
 */
class GlyphLayoutTest(context: Context) : Game<Scene>(context) {

    val font by load<TtfFont>(resourcesVfs["LiberationSans-Regular.ttf"])

    override fun create() {
        val layout = GlyphLayout()
        layout.setText(font, "My test string", 500f, scale = font.pxScale(36), wrap = true)
        println(layout.runs)
    }

    override fun update(dt: Duration) {
        gl.clearColor(Color.DARK_GRAY)
    }

}