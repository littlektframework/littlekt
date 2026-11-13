package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader

/**
 * A [Font] whose glyphs are drawn from textures, such as a [BitmapFont] or a [RasterFont]. Each
 * [GlyphMetrics.page] is an index into [textures].
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface TextureFont : Font {
    /** The textures of each page. Pages may be added as glyphs are rasterized. */
    val textures: List<Texture>

    /** The batch shader this font must be drawn with, or `null` for the batch's own shader. */
    val shader: Shader?
        get() = null

    /**
     * @return this font for drawing at [scale] pixels per unit. A font that can be rasterized
     *   returns a font for the scaled size, so text stays sharp. Draw it at `1 / scale` so it keeps
     *   the same size.
     */
    fun forScale(scale: Float): TextureFont = this
}
