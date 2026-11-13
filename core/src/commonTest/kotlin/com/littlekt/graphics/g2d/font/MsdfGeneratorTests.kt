package com.littlekt.graphics.g2d.font

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MsdfGeneratorTests {

    private fun rect(path: GlyphPath, x0: Float, y0: Float, x1: Float, y1: Float, clockwise: Boolean = false) {
        path.moveTo(x0, y0)
        if (clockwise) {
            path.lineTo(x0, y1)
            path.lineTo(x1, y1)
            path.lineTo(x1, y0)
        } else {
            path.lineTo(x1, y0)
            path.lineTo(x1, y1)
            path.lineTo(x0, y1)
        }
        path.close()
    }

    private fun generate(path: GlyphPath, size: Int = 32, range: Float = 4f) =
        MsdfGenerator().apply { generate(path, size, size, 1f, 0f, size.toFloat(), range) }

    private fun assertInsideMatches(generator: MsdfGenerator, inside: (x: Int, y: Int) -> Boolean) {
        for (y in 0 until generator.height) {
            for (x in 0 until generator.width) {
                val expected = inside(x, y)
                assertEquals(expected, generator.medianAt(x, y) > 0.5f, "msdf at $x,$y")
                assertEquals(expected, generator.valueAt(x, y, 3) > 0.5f, "sdf at $x,$y")
            }
        }
    }

    @Test
    fun test_square_inside_and_outside() {
        val path = GlyphPath().also { rect(it, 8f, 8f, 24f, 24f) }
        val generator = generate(path)
        assertInsideMatches(generator) { x, y -> x in 8..23 && (31 - y) in 8..23 }
    }

    @Test
    fun test_winding_direction_does_not_matter() {
        val ccw = generate(GlyphPath().also { rect(it, 8f, 8f, 24f, 24f) })
        val cw = generate(GlyphPath().also { rect(it, 8f, 8f, 24f, 24f, clockwise = true) })
        for (y in 0 until 32) for (x in 0 until 32) {
            assertTrue(abs(ccw.medianAt(x, y) - cw.medianAt(x, y)) < 1e-4f, "at $x,$y")
        }
    }

    @Test
    fun test_distance_falls_off_over_the_range() {
        val generator = generate(GlyphPath().also { rect(it, 8f, 8f, 24f, 24f) }, range = 4f)
        val row = 31 - 16
        assertTrue(abs(generator.valueAt(8, row, 3) - (0.5f + 0.5f / 4f)) < 0.01f)
        assertTrue(abs(generator.valueAt(7, row, 3) - (0.5f - 0.5f / 4f)) < 0.01f)
        assertTrue(abs(generator.valueAt(16, row, 3) - (0.5f + 7.5f / 4f)) < 0.01f)
        assertTrue(abs(generator.valueAt(1, row, 3) - (0.5f - 6.5f / 4f)) < 0.01f)
    }

    @Test
    fun test_hole_is_outside() {
        val path = GlyphPath().also {
            rect(it, 4f, 4f, 28f, 28f)
            rect(it, 12f, 12f, 20f, 20f, clockwise = true)
        }
        val generator = generate(path)
        assertInsideMatches(generator) { x, y ->
            val fy = 31 - y
            val outer = x in 4..27 && fy in 4..27
            val hole = x in 12..19 && fy in 12..19
            outer && !hole
        }
    }

    @Test
    fun test_overlapping_contours_are_filled() {
        val path = GlyphPath().also {
            rect(it, 4f, 4f, 20f, 20f)
            rect(it, 12f, 12f, 28f, 28f)
        }
        val generator = generate(path)
        assertInsideMatches(generator) { x, y ->
            val fy = 31 - y
            (x in 4..19 && fy in 4..19) || (x in 12..27 && fy in 12..27)
        }
    }

    @Test
    fun test_curves_are_supported() {
        val path = GlyphPath().apply {
            moveTo(4f, 16f)
            quadTo(16f, 40f, 28f, 16f)
            quadTo(16f, -8f, 4f, 16f)
            close()
        }
        val generator = generate(path)
        assertTrue(generator.medianAt(16, 31 - 16) > 0.9f)
        assertTrue(generator.medianAt(1, 1) < 0.1f)
    }

    @Test
    fun test_write_bytes() {
        val generator = generate(GlyphPath().also { rect(it, 8f, 8f, 24f, 24f) })
        val out = ByteArray(32 * 32 * 4)
        generator.write(out)
        val center = ((31 - 16) * 32 + 16) * 4
        assertEquals(255, out[center + 3].toInt() and 0xFF)
        assertEquals(0, out[3].toInt() and 0xFF)
    }
}
