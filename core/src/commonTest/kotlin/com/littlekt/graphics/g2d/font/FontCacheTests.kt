package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.math.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class FontCacheTests {

    private class TestFont : Font {
        override val metrics =
            FontMetrics(
                size = 10f,
                ascent = 2f,
                baseline = 8f,
                lineHeight = 10f,
                descent = -2f,
                capHeight = 6f,
            )
        override val glyphMetrics: Map<Int, GlyphMetrics> =
            listOf('a', 'b', ' ').associate {
                it.code to
                    GlyphMetrics(
                        size = 10f,
                        code = it.code,
                        bounds = Rect(0f, -8f, 5f, 8f),
                        xAdvance = 6f,
                        u0 = 0.1f,
                        v0 = 0.2f,
                        u1 = 0.3f,
                        v1 = 0.4f,
                    )
            }
        override var wrapChars: CharSequence = ""

        override fun getKerning(first: Int, second: Int): Kerning? = null

        override fun release() = Unit
    }

    private class TestCache : FontCache() {
        val vertices
            get() = pageVertices[0]
    }

    private fun cacheOf(text: String): TestCache {
        val layout = GlyphLayout()
        layout.setText(TestFont(), text, Color.WHITE)
        return TestCache().apply { setText(layout, 0f, 0f) }
    }

    private fun assertColor(cache: TestCache, color: Color) {
        val vertices = cache.vertices
        for (vertex in 0 until vertices.size / 9) {
            val index = vertex * 9 + 3
            assertEquals(color.r, vertices[index], "r of vertex $vertex")
            assertEquals(color.g, vertices[index + 1], "g of vertex $vertex")
            assertEquals(color.b, vertices[index + 2], "b of vertex $vertex")
            assertEquals(color.a, vertices[index + 3], "a of vertex $vertex")
        }
    }

    @Test
    fun test_tint_sets_every_vertex_color() {
        val cache = cacheOf("ab")
        cache.tint(Color.RED)
        assertEquals(72, cache.vertices.size)
        assertColor(cache, Color.RED)
    }

    @Test
    fun test_tint_keeps_positions_and_uvs() {
        val cache = cacheOf("ab")
        val before = FloatArray(cache.vertices.size) { cache.vertices[it] }
        cache.tint(Color.BLUE)
        for (i in before.indices) {
            if (i % 9 in 3..6) continue
            assertEquals(before[i], cache.vertices[i], "index $i")
        }
    }

    @Test
    fun test_tint_with_reused_mutable_color_applies_changes() {
        val cache = cacheOf("ab")
        val color = MutableColor(Color.RED)
        cache.tint(color)
        color.set(Color.GREEN)
        cache.tint(color)
        assertColor(cache, Color.GREEN)
    }

    @Test
    fun test_set_text_resets_tint() {
        val cache = cacheOf("a")
        cache.tint(Color.RED)
        val layout = GlyphLayout()
        layout.setText(TestFont(), "b", Color.WHITE)
        cache.setText(layout, 0f, 0f)
        cache.tint(Color.RED)
        assertColor(cache, Color.RED)
    }

    @Test
    fun test_glyph_layout_height_of_one_line_is_cap_height() {
        val layout = GlyphLayout()
        layout.setText(TestFont(), "ab", Color.WHITE)
        assertEquals(6f, layout.height)
    }

    @Test
    fun test_glyph_layout_height_adds_a_line_height_per_line() {
        val layout = GlyphLayout()
        layout.setText(TestFont(), "ab\nab\nab", Color.WHITE)
        assertEquals(26f, layout.height)
    }

    @Test
    fun test_glyph_layout_height_counts_wrapped_lines() {
        val layout = GlyphLayout()
        layout.setText(TestFont(), "ab ab", Color.WHITE, width = 20f, wrap = true)
        assertEquals(2, layout.runs.size)
        assertEquals(16f, layout.height)
    }
}
