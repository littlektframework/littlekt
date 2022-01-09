package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 1/6/2022
 */
class BitmapFontCache(val font: BitmapFont) : FontCache(font.pages) {

    fun draw(batch: SpriteBatch) {
        draw(batch, font.textures)
    }

    fun setText(
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) = setText(font, text, x, y, scale, rotation, color, targetWidth, align, wrap)


    fun addText(
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) = addText(font, text, x, y, scale, rotation, color, targetWidth, align, wrap)

}