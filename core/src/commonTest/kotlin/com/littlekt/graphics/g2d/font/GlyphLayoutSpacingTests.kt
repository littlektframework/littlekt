package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.math.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlyphLayoutSpacingTests {

    private val font =
        object : Font {
            override val metrics =
                FontMetrics(size = 20f, baseline = 15f, lineHeight = 20f, descent = -5f, capHeight = 12f)
            override val glyphMetrics: Map<Int, GlyphMetrics> =
                "AB ".associate {
                    it.code to GlyphMetrics(20f, it.code, Rect(0f, -15f, 10f, 12f), xAdvance = 11f)
                }
            override var wrapChars: CharSequence = ""

            override fun getKerning(first: Int, second: Int): Kerning? = null
        }

    private fun glyphXs(layout: GlyphLayout, run: Int = 0): List<Float> {
        val r = layout.runs[run]
        var x = r.x
        return r.glyphs.indices.map { i ->
            x += r.advances[i]
            x
        }
    }

    @Test
    fun test_letter_spacing_is_added_between_glyphs() {
        val layout = GlyphLayout()
        layout.setText(font, "AAB", letterSpacing = 4f)
        assertEquals(listOf(0f, 15f, 30f), glyphXs(layout))
        assertEquals(40f, layout.width, 0.001f)
    }

    @Test
    fun test_letter_spacing_doesnt_trail_the_last_glyph() {
        val plain = GlyphLayout().apply { setText(font, "AB") }
        val spaced = GlyphLayout().apply { setText(font, "AB", letterSpacing = 6f) }
        assertEquals(plain.width + 6f, spaced.width, 0.001f)
    }

    @Test
    fun test_letter_spacing_keeps_center_and_right_alignment() {
        val center = GlyphLayout()
        center.setText(font, "AB", width = 100f, align = HAlign.CENTER, letterSpacing = 10f)
        val run = center.runs.first()
        assertEquals(31f, run.width, 0.001f)
        assertEquals((100f - run.width) * 0.5f, run.x, 0.001f)

        val right = GlyphLayout()
        right.setText(font, "AB", width = 100f, align = HAlign.RIGHT, letterSpacing = 10f)
        val rightRun = right.runs.first()
        assertEquals(100f, rightRun.x + rightRun.width, 0.001f)
    }

    @Test
    fun test_letter_spacing_isnt_scaled() {
        val layout = GlyphLayout()
        layout.setText(font, "AA", scaleX = 2f, letterSpacing = 3f)
        assertEquals(listOf(0f, 25f), glyphXs(layout))
    }

    @Test
    fun test_line_spacing_is_added_between_lines() {
        val plain = GlyphLayout().apply { setText(font, "A\nA\nA") }
        val spaced = GlyphLayout().apply { setText(font, "A\nA\nA", lineSpacing = 5f) }
        assertEquals(plain.runs[0].y, spaced.runs[0].y, 0.001f)
        assertEquals(plain.runs[1].y - 5f, spaced.runs[1].y, 0.001f)
        assertEquals(plain.runs[2].y - 10f, spaced.runs[2].y, 0.001f)
        assertEquals(plain.height + 10f, spaced.height, 0.001f)
    }

    @Test
    fun test_line_spacing_applies_to_wrapped_lines() {
        val layout = GlyphLayout()
        layout.setText(font, "AB AB AB AB AB AB", width = 65f, wrap = true, lineSpacing = 7f)
        assertTrue(layout.runs.size > 1)
        for (i in 1 until layout.runs.size) {
            assertEquals(27f, layout.runs[i - 1].y - layout.runs[i].y, 0.001f)
        }
    }

    @Test
    fun test_justify_stretches_wrapped_lines_to_the_width() {
        val layout = GlyphLayout()
        layout.setText(font, "AB AB AB AB AB AB AB", Color.WHITE, 70f, wrap = true, justify = true)
        assertTrue(layout.runs.size > 1)
        for (i in 0 until layout.runs.size - 1) {
            val run = layout.runs[i]
            assertTrue(run.wrapped)
            assertEquals(70f, run.x + run.width, 0.001f)
        }
    }

    @Test
    fun test_justify_keeps_the_alignment_of_the_last_line() {
        val layout = GlyphLayout()
        layout.setText(
            font,
            "AB AB AB AB AB",
            Color.WHITE,
            70f,
            align = HAlign.RIGHT,
            wrap = true,
            justify = true,
        )
        val last = layout.runs.last()
        assertFalse(last.wrapped)
        assertEquals(70f, last.x + last.width, 0.001f)
        assertTrue(last.width < 70f)
        val first = layout.runs.first()
        assertEquals(0f, first.x, 0.001f)
    }

    @Test
    fun test_justify_doesnt_stretch_lines_ended_by_a_new_line() {
        val layout = GlyphLayout()
        layout.setText(font, "AB AB\nAB", Color.WHITE, 200f, wrap = true, justify = true)
        assertEquals(2, layout.runs.size)
        assertTrue(layout.runs[0].width < 200f)
    }

    @Test
    fun test_justify_with_letter_spacing_still_reaches_the_width() {
        val layout = GlyphLayout()
        layout.setText(
            font,
            "AB AB AB AB AB AB",
            Color.WHITE,
            80f,
            wrap = true,
            letterSpacing = 2f,
            justify = true,
        )
        for (i in 0 until layout.runs.size - 1) {
            val run = layout.runs[i]
            assertEquals(80f, run.x + run.width, 0.001f)
        }
    }

    @Test
    fun test_truncate_with_letter_spacing_fits_the_width() {
        val layout = GlyphLayout()
        layout.setText(font, "ABABABABAB", width = 60f, truncate = "A", letterSpacing = 2f)
        assertTrue(layout.width <= 60f)
    }
}
