package com.littlekt.graph.node.ui

import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.FontCache
import com.littlekt.graphics.g2d.font.TextureFont
import kotlin.math.round

/**
 * Keeps the text of a [Control] drawn with the font for its current pixel scale, so fonts that can
 * be rasterized, such as a [com.littlekt.graphics.g2d.font.RasterFont], stay sharp when the canvas
 * is scaled. Glyph positions are in the control's units; [ratio] converts the font back to them.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
internal class ScaledText {
    val cache = FontCache()

    /** The font the text is laid out and drawn with. */
    var font: TextureFont? = null
        private set

    /** The pixel size of [font] compared to the theme font. Divide font scales by this. */
    var ratio = 1f
        private set

    /** The pixel scale [font] was chosen for. */
    var pixelScale = 1f
        private set

    /** Picks the font for [base] at [scale] pixels per unit. */
    fun update(base: TextureFont, scale: Float): TextureFont {
        val sized = base.forScale(scale)
        val baseSize = base.metrics.size
        ratio = if (sized === base || baseSize <= 0f) 1f else sized.metrics.size / baseSize
        pixelScale = scale
        font = sized
        return sized
    }

    /** @return [value] rounded to the nearest pixel. */
    fun snap(value: Float): Float = if (pixelScale <= 0f) value else round(value * pixelScale) / pixelScale

    fun draw(batch: Batch) {
        val font = font ?: return
        cache.draw(batch, font.textures)
    }
}
