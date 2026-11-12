package com.littlekt.graphics.g2d.font

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlyphRasterizerTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun rect(x0: Float, y0: Float, x1: Float, y1: Float, clockwise: Boolean = false) =
        GlyphPath().apply {
            moveTo(x0, y0)
            if (clockwise) {
                lineTo(x0, y1)
                lineTo(x1, y1)
                lineTo(x1, y0)
            } else {
                lineTo(x1, y0)
                lineTo(x1, y1)
                lineTo(x0, y1)
            }
            close()
        }

    private fun rasterize(path: GlyphPath, size: Int = 6) =
        GlyphRasterizer().apply { rasterize(path, size, size, 1f, 0f, size.toFloat()) }

    @Test
    fun test_pixel_aligned_square_is_fully_covered() {
        val r = rasterize(rect(1f, 1f, 4f, 4f))
        assertNear(1f, r.coverageAt(2, 3))
        assertNear(1f, r.coverageAt(1, 4))
        assertNear(0f, r.coverageAt(0, 3))
        assertNear(0f, r.coverageAt(4, 3))
        assertNear(0f, r.coverageAt(2, 5))
    }

    @Test
    fun test_winding_direction_does_not_matter() {
        val ccw = rasterize(rect(1f, 1f, 4f, 4f))
        val cw = rasterize(rect(1f, 1f, 4f, 4f, clockwise = true))
        for (y in 0 until 6) for (x in 0 until 6) {
            assertNear(ccw.coverageAt(x, y), cw.coverageAt(x, y))
        }
    }

    @Test
    fun test_half_pixel_edges_are_half_covered() {
        val r = rasterize(rect(1.5f, 1f, 3.5f, 4f))
        assertNear(0.5f, r.coverageAt(1, 3))
        assertNear(1f, r.coverageAt(2, 3))
        assertNear(0.5f, r.coverageAt(3, 3))
    }

    @Test
    fun test_diagonal_edge_covers_triangle_area() {
        val path =
            GlyphPath().apply {
                moveTo(0f, 0f)
                lineTo(4f, 0f)
                lineTo(4f, 4f)
                close()
            }
        val r = rasterize(path, 4)
        assertNear(0.5f, r.coverageAt(0, 3))
        assertNear(1f, r.coverageAt(3, 3))
        assertNear(0f, r.coverageAt(0, 0))
        var total = 0f
        for (y in 0 until 4) for (x in 0 until 4) total += r.coverageAt(x, y)
        assertNear(8f, total, 1e-3f)
    }

    @Test
    fun test_quadratic_curve_area_matches_parabola() {
        val path =
            GlyphPath().apply {
                moveTo(0f, 0f)
                lineTo(8f, 0f)
                quadTo(4f, 8f, 0f, 0f)
                close()
            }
        val r = rasterize(path, 8)
        var total = 0f
        for (y in 0 until 8) for (x in 0 until 8) total += r.coverageAt(x, y)
        assertNear(8f * 4f * 2f / 3f, total, 0.7f)
    }

    @Test
    fun test_coverage_bytes_with_stride() {
        val r = rasterize(rect(1f, 1f, 4f, 4f))
        val out = ByteArray(10 * 6)
        r.coverage(out, offset = 2, stride = 10)
        assertEquals(255, out[2 + 3 * 10 + 2].toInt() and 0xFF)
        assertEquals(0, out[2 + 3 * 10].toInt() and 0xFF)
    }

    @Test
    fun test_overlapping_contours_use_nonzero_fill() {
        val path = rect(1f, 1f, 4f, 4f).apply { extend(rect(2f, 2f, 5f, 5f)) }
        val r = rasterize(path)
        assertNear(1f, r.coverageAt(2, 3))
        assertNear(1f, r.coverageAt(3, 2))
    }
}
