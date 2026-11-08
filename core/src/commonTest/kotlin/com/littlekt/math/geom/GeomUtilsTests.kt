package com.littlekt.math.geom

import com.littlekt.math.Vec2f
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeomUtilsTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_crossing_segments_distance_is_zero() {
        assertNear(
            0f,
            closestPointsBetweenSegments(Vec2f(-1f, 0f), Vec2f(1f, 0f), Vec2f(0f, -1f), Vec2f(0f, 1f))
        )
    }

    @Test
    fun test_parallel_segments_distance() {
        assertNear(
            2f,
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(4f, 0f), Vec2f(1f, 2f), Vec2f(3f, 2f))
        )
    }

    @Test
    fun test_collinear_disjoint_segments_distance() {
        assertNear(
            3f,
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(1f, 0f), Vec2f(4f, 0f), Vec2f(6f, 0f))
        )
    }

    @Test
    fun test_endpoint_to_segment_distance() {
        assertNear(
            1f,
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(4f, 0f), Vec2f(2f, 1f), Vec2f(2f, 5f))
        )
    }

    @Test
    fun test_endpoint_to_endpoint_distance() {
        assertNear(
            sqrt(2f),
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(1f, 0f), Vec2f(2f, 1f), Vec2f(3f, 3f))
        )
    }

    @Test
    fun test_segment_distance_is_symmetric() {
        val a = closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(3f, 1f), Vec2f(5f, 4f), Vec2f(2f, 6f))
        val b = closestPointsBetweenSegments(Vec2f(5f, 4f), Vec2f(2f, 6f), Vec2f(0f, 0f), Vec2f(3f, 1f))
        assertNear(a, b)
    }

    @Test
    fun test_degenerate_segments() {
        assertNear(
            5f,
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(0f, 0f), Vec2f(3f, 4f), Vec2f(3f, 4f))
        )
        assertNear(
            2f,
            closestPointsBetweenSegments(Vec2f(1f, 2f), Vec2f(1f, 2f), Vec2f(0f, 0f), Vec2f(4f, 0f))
        )
        assertNear(
            2f,
            closestPointsBetweenSegments(Vec2f(0f, 0f), Vec2f(4f, 0f), Vec2f(1f, 2f), Vec2f(1f, 2f))
        )
    }

    @Test
    fun test_is_clockwise() {
        val ccw = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        val cw = floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
        assertFalse(isClockwise(ccw))
        assertTrue(isCCW(ccw))
        assertTrue(isClockwise(cw))
        assertFalse(isCCW(cw))
    }

    @Test
    fun test_is_clockwise_with_offset_and_count() {
        val data = floatArrayOf(99f, 99f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 99f, 99f)
        assertTrue(isClockwise(data, 2, 8))
    }

    @Test
    fun test_is_clockwise_concave_polygon() {
        val ccw = floatArrayOf(0f, 0f, 4f, 0f, 4f, 4f, 2f, 1f, 0f, 4f)
        assertFalse(isClockwise(ccw))
    }

    @Test
    fun test_is_clockwise_too_few_points() {
        assertFalse(isClockwise(floatArrayOf(0f, 0f)))
    }

    @Test
    fun test_point_and_mutable_point() {
        val p = Point(1, 2)
        assertEquals(1f, p.x)
        assertEquals(2f, p.y)
        val mp = MutablePoint(3f, 4f)
        mp.x = 5f
        mp.y = 6f
        assertEquals(5f, mp.x)
        assertEquals(6f, (mp as Point).y)
        assertEquals(0f, Point.ZERO.x)
    }
}
