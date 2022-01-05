package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graphics.TextureSlice

/**
 * @author Colt Daily
 * @date 1/5/22
 */
class BitmapFont : Font() {
    override val glyphs: Map<Int, Glyph> = mutableMapOf()

    private val slices = mutableListOf<TextureSlice>()
    private val cache = FontCache()
}
