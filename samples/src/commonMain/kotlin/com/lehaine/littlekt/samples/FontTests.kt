package com.lehaine.littlekt.samples

import com.lehaine.littlekt.BitmapFontAssetParameter
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/3/2022
 */
class FontTests(context: Context) : Game<Scene>(context) {

    val libSans by load<TtfFont>(resourcesVfs["LiberationSans-Regular.ttf"])
    val pixelFont by load<BitmapFont>(resourcesVfs["m5x7_16.fnt"])
    val barlowTiny by load<BitmapFont>(
        resourcesVfs["barlow_condensed_medium_regular_9.fnt"],
        BitmapFontAssetParameter(magFilter = TexMagFilter.LINEAR)
    )
    val barlowSmall by load<BitmapFont>(
        resourcesVfs["barlow_condensed_medium_regular_11.fnt"],
        BitmapFontAssetParameter(magFilter = TexMagFilter.LINEAR)
    )
    val barlowNormal by load<BitmapFont>(
        resourcesVfs["barlow_condensed_medium_regular_17.fnt"],
        BitmapFontAssetParameter(magFilter = TexMagFilter.LINEAR)
    )
    val barlowLarge by load<BitmapFont>(
        resourcesVfs["barlow_condensed_medium_regular_32.fnt"],
        BitmapFontAssetParameter(magFilter = TexMagFilter.LINEAR)
    )
    val batch = SpriteBatch(this)

    val camera = OrthographicCamera(context.graphics.width, context.graphics.height)

    override fun create() {
        val layout = GlyphLayout()
        layout.setText(libSans, "my test string!!", 200f, scale = 36f, align = HAlign.CENTER, wrap = true)
    }

    override fun update(dt: Duration) {
        gl.clearColor(Color.DARK_GRAY)

        camera.update()
        batch.use(camera.viewProjection) {
            pixelFont.draw(it, "This is my bitmap!", 0f, 0f)
            barlowTiny.draw(it, "This is barlow point 9", 50f, 150f, color = Color.WHITE)
            barlowSmall.draw(it, "This is barlow point 11", 50f, 200f, color = Color.WHITE)
            barlowNormal.draw(it, "This is barlow point 17", 50f, 250f, color = Color.WHITE)
            barlowLarge.draw(it, "This is barlow point 32", 50f, 350f, color = Color.WHITE)

            it.draw(Textures.white, 0f, 200f, scaleX = 10f, scaleY = 10f)
        }
    }

}