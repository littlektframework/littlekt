package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.VAlign
import com.littlekt.math.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FontCacheAlignTests {

    private val font =
        object : Font {
            override val metrics =
                FontMetrics(size = 20f, baseline = 15f, lineHeight = 20f, descent = -5f, capHeight = 12f)
            override val glyphMetrics: Map<Int, GlyphMetrics> =
                "AB".associate {
                    it.code to GlyphMetrics(20f, it.code, Rect(0f, -15f, 10f, 12f), xAdvance = 11f)
                }
            override var wrapChars: CharSequence = ""

            override fun getKerning(first: Int, second: Int): Kerning? = null
        }

    private class Bounds : FontCache() {
        fun minY(): Float = ys().min()

        fun maxY(): Float = ys().max()

        private fun ys(): List<Float> {
            val vertices = pageVertices[0]
            return (0 until vertices.size step 9).map { vertices[it + 1] }
        }
    }

    private fun bounds(align: VAlign?, text: String = "AB"): Bounds =
        Bounds().apply { setText(font, text, 0f, 0f, verticalAlign = align) }

    @Test
    fun test_top_puts_the_lines_below_y() {
        val text = bounds(VAlign.TOP)
        assertEquals(-3f, text.maxY(), 0.001f)
        assertEquals(-15f, text.minY(), 0.001f)
    }

    @Test
    fun test_bottom_puts_the_lines_above_y() {
        val text = bounds(VAlign.BOTTOM)
        assertEquals(5f, text.minY(), 0.001f)
        assertEquals(17f, text.maxY(), 0.001f)
    }

    @Test
    fun test_center_centers_the_line_boxes_on_y() {
        val text = bounds(VAlign.CENTER)
        assertEquals(-5f, text.minY(), 0.001f)
        assertEquals(7f, text.maxY(), 0.001f)
    }

    @Test
    fun test_null_keeps_the_first_line_bottom_at_y() {
        assertTrue(abs(bounds(null).minY() - bounds(VAlign.BOTTOM).minY()) < 0.001f)
    }
}
