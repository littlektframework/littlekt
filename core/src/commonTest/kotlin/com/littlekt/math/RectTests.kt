package com.littlekt.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RectTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-3f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_x2_and_y2() {
        val rect = Rect(1f, 2f, 3f, 4f)
        assertEquals(4f, rect.x2)
        assertEquals(6f, rect.y2)
    }

    @Test
    fun test_is_empty() {
        assertTrue(Rect().isEmpty)
        assertTrue(Rect(0f, 0f, 10f, 0f).isEmpty)
        assertTrue(Rect(0f, 0f, 0f, 10f).isEmpty)
        assertTrue(Rect(0f, 0f, -1f, 10f).isEmpty)
        assertFalse(Rect(0f, 0f, 1f, 1f).isEmpty)
    }

    @Test
    fun test_set_returns_same_instance() {
        val rect = Rect()
        val result = rect.set(1f, 2f, 3f, 4f)
        assertTrue(result === rect)
        assertEquals(Rect(1f, 2f, 3f, 4f), rect)
    }

    @Test
    fun test_from_bounds() {
        val rect = Rect.fromBounds(1f, 2f, 5f, 8f)
        assertEquals(Rect(1f, 2f, 4f, 6f), rect)
        assertEquals(5f, rect.x2)
        assertEquals(8f, rect.y2)
    }

    @Test
    fun test_from_bounds_with_swapped_corners_is_empty() {
        assertTrue(Rect.fromBounds(5f, 8f, 1f, 2f).isEmpty)
    }

    @Test
    fun test_intersects_partial_overlap() {
        val a = Rect(0f, 0f, 10f, 10f)
        val b = Rect(5f, 5f, 10f, 10f)
        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
    }

    @Test
    fun test_intersects_partial_overlap_on_single_axis_only() {
        val a = Rect(0f, 0f, 10f, 10f)
        assertFalse(a.intersects(Rect(5f, 20f, 10f, 10f)))
        assertFalse(a.intersects(Rect(20f, 5f, 10f, 10f)))
        assertFalse(a.intersects(Rect(5f, -20f, 10f, 10f)))
        assertFalse(a.intersects(Rect(-20f, 5f, 10f, 10f)))
    }

    @Test
    fun test_intersects_overlap_from_each_side() {
        val a = Rect(0f, 0f, 10f, 10f)
        assertTrue(a.intersects(Rect(-5f, 2f, 10f, 2f)))
        assertTrue(a.intersects(Rect(5f, 2f, 10f, 2f)))
        assertTrue(a.intersects(Rect(2f, -5f, 2f, 10f)))
        assertTrue(a.intersects(Rect(2f, 5f, 2f, 10f)))
    }

    @Test
    fun test_intersects_thin_rect_crossing_through() {
        val a = Rect(0f, 0f, 10f, 10f)
        assertTrue(a.intersects(Rect(-5f, 4f, 20f, 1f)))
        assertTrue(a.intersects(Rect(4f, -5f, 1f, 20f)))
    }

    @Test
    fun test_intersects_contained() {
        val outer = Rect(0f, 0f, 10f, 10f)
        val inner = Rect(2f, 3f, 1f, 1f)
        assertTrue(outer.intersects(inner))
        assertTrue(inner.intersects(outer))
    }

    @Test
    fun test_intersects_disjoint() {
        assertFalse(Rect(0f, 0f, 1f, 1f).intersects(Rect(5f, 5f, 1f, 1f)))
    }

    @Test
    fun test_touching_edges_do_not_intersect() {
        val a = Rect(0f, 0f, 10f, 10f)
        assertFalse(a.intersects(Rect(10f, 0f, 5f, 10f)))
        assertFalse(a.intersects(Rect(-5f, 0f, 5f, 10f)))
        assertFalse(a.intersects(Rect(0f, 10f, 10f, 5f)))
        assertFalse(a.intersects(Rect(0f, -5f, 10f, 5f)))
        assertFalse(a.intersects(Rect(10f, 10f, 5f, 5f)))
    }

    @Test
    fun test_intersects_corner_coordinates() {
        val a = Rect(0f, 0f, 10f, 10f)
        assertTrue(a.intersects(left = 8f, bottom = -2f, right = 12f, top = 1f))
        assertFalse(a.intersects(left = 8f, bottom = 11f, right = 12f, top = 15f))
        assertFalse(a.intersects(left = 10f, bottom = 0f, right = 12f, top = 10f))
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(Rect(1f, 2f, 3f, 4f), Rect(1f, 2f, 3f, 4f))
        assertEquals(Rect(1f, 2f, 3f, 4f).hashCode(), Rect(1f, 2f, 3f, 4f).hashCode())
        assertNotEquals(Rect(1f, 2f, 3f, 4f), Rect(1f, 2f, 3f, 5f))
    }

    @Test
    fun test_builder_empty_until_included() {
        val builder = RectBuilder()
        assertTrue(builder.isEmpty())
        builder.include(1f, 2f)
        assertFalse(builder.isEmpty())
        builder.reset()
        assertTrue(builder.isEmpty())
    }

    @Test
    fun test_builder_include_points() {
        val builder = RectBuilder()
        builder.include(1f, 5f)
        builder.include(-2f, 3f)
        builder.include(4, -1)
        assertEquals(Rect(-2f, -1f, 6f, 6f), builder.build())
    }

    @Test
    fun test_builder_include_x_and_y_separately() {
        val builder = RectBuilder()
        builder.includeX(2f)
        builder.includeX(-3)
        builder.includeY(1f)
        builder.includeY(7)
        assertEquals(Rect(-3f, 1f, 5f, 6f), builder.build())
    }

    @Test
    fun test_builder_bezier_includes_y_extremum() {
        val builder = RectBuilder()
        builder.addBezier(0f, 0f, 0f, 10f, 10f, 10f, 10f, 0f)
        val rect = builder.build()
        assertNear(0f, rect.x)
        assertNear(10f, rect.x2)
        assertNear(0f, rect.y)
        assertNear(7.5f, rect.y2)
    }

    @Test
    fun test_builder_bezier_includes_x_extremum() {
        val builder = RectBuilder()
        builder.addBezier(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val rect = builder.build()
        assertNear(0f, rect.x)
        assertNear(7.5f, rect.x2)
        assertNear(0f, rect.y)
        assertNear(10f, rect.y2)
    }

    @Test
    fun test_builder_bezier_with_small_discriminant() {
        val builder = RectBuilder()
        builder.addBezier(0f, 0f, 1f / 3f, 0.1f, 2f / 3f, 0.05f, 1f, 0f)
        assertNear(0.0577f, builder.build().y2, 1e-4f)
    }

    @Test
    fun test_builder_bezier_straight_line_only_includes_endpoints() {
        val builder = RectBuilder()
        builder.addBezier(0f, 0f, 1f, 1f, 2f, 2f, 3f, 3f)
        assertEquals(Rect(0f, 0f, 3f, 3f), builder.build())
    }

    @Test
    fun test_builder_quad_includes_extremum() {
        val builder = RectBuilder()
        builder.addQuad(0f, 0f, 5f, 10f, 10f, 0f)
        val rect = builder.build()
        assertNear(0f, rect.x)
        assertNear(10f, rect.x2)
        assertNear(0f, rect.y)
        assertNear(5f, rect.y2)
    }
}
