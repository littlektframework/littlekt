package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.font.FontMetrics
import com.littlekt.graphics.g2d.font.GlyphMetrics
import com.littlekt.graphics.g2d.font.Kerning
import com.littlekt.graphics.g2d.font.TextureFont

/** A font with no textures whose glyphs are each 6 wide, for laying out text in tests. */
internal object TestFont : TextureFont {
    override val textures: List<Texture> = emptyList()
    override val metrics = FontMetrics(size = 10f, top = 10f, ascent = 8f, baseline = 8f, lineHeight = 12f)
    override val glyphMetrics: Map<Int, GlyphMetrics> = (32..126).associateWith { GlyphMetrics(size = 10f, code = it, xAdvance = 6f) }
    override var wrapChars: CharSequence = ""

    override fun getKerning(first: Int, second: Int): Kerning? = null

    override fun getKerning(first: Char, second: Char): Kerning? = null

    override fun release() = Unit
}

/** Installs an empty default theme on [TestFont] so text controls work without internal resources. */
internal fun useTestTheme() {
    Theme.defaultTheme = Theme(defaultFont = TestFont)
}
