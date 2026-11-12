package com.littlekt.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathUtilsTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Double, actual: Double, eps: Double = 1e-9) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_deg_rad_conversions() {
        assertNear(PI_F, 180f.toRad())
        assertNear(90f, (PI_F / 2f).toDeg())
        assertNear(PI, 180.0.toRad())
        assertNear(45.0, (PI / 4.0).toDeg())
    }

    @Test
    fun test_is_fuzzy_zero() {
        assertTrue(0f.isFuzzyZero())
        assertTrue(1e-6f.isFuzzyZero())
        assertTrue((-1e-6f).isFuzzyZero())
        assertFalse(1e-4f.isFuzzyZero())
        assertTrue(1e-11.isFuzzyZero())
        assertFalse(1e-9.isFuzzyZero())
    }

    @Test
    fun test_is_fuzzy_equal() {
        assertTrue(isFuzzyEqual(1f, 1f + 1e-6f))
        assertFalse(isFuzzyEqual(1f, 1.001f))
        assertTrue(isFuzzyEqual(1f, 1.001f, 0.01f))
        assertTrue(1.0.isFuzzyEqual(1.0 + 1e-12))
        assertFalse(1.0.isFuzzyEqual(1.0 + 1e-8))
        assertTrue(0.1f + 0.2f ife 0.3f)
        assertTrue(0.1 + 0.2 ife 0.3)
    }

    @Test
    fun test_clamp_int() {
        assertEquals(0, (-5).clamp(0, 10))
        assertEquals(10, 15.clamp(0, 10))
        assertEquals(5, 5.clamp(0, 10))
        assertEquals(0, 0.clamp(0, 10))
        assertEquals(10, 10.clamp(0, 10))
    }

    @Test
    fun test_clamp_float_defaults_to_unit_range() {
        assertEquals(0f, (-0.5f).clamp())
        assertEquals(1f, 1.5f.clamp())
        assertEquals(0.25f, 0.25f.clamp())
        assertEquals(-2f, (-3f).clamp(-2f, 2f))
    }

    @Test
    fun test_clamp_double() {
        assertEquals(0.0, (-0.5).clamp())
        assertEquals(1.0, 1.5.clamp())
        assertEquals(3.0, 5.0.clamp(-3.0, 3.0))
    }

    @Test
    fun test_wrap_int() {
        assertEquals(9, (-1).wrap(0, 10))
        assertEquals(0, 10.wrap(0, 10))
        assertEquals(5, 5.wrap(0, 10))
        assertEquals(3, 13.wrap(0, 10))
        assertEquals(-4, 6.wrap(-5, 5))
    }

    @Test
    fun test_wrap_float() {
        assertNear(350f, (-10f).wrap(0f, 360f))
        assertNear(10f, 370f.wrap(0f, 360f))
        assertNear(0f, 360f.wrap(0f, 360f))
    }

    @Test
    fun test_wrap_double() {
        assertNear(0.5, 2.5.wrap(0.0, 1.0))
        assertNear(0.75, (-0.25).wrap(0.0, 1.0))
    }

    @Test
    fun test_num_mip_levels() {
        assertEquals(1, getNumMipLevels(1, 1))
        assertEquals(9, getNumMipLevels(256, 128))
        assertEquals(11, getNumMipLevels(1000, 1024))
        assertEquals(10, getNumMipLevels(1000, 1))
    }

    @Test
    fun test_smooth_step() {
        assertEquals(0f, smoothStep(0f, 1f, -1f))
        assertEquals(1f, smoothStep(0f, 1f, 2f))
        assertNear(0.5f, smoothStep(0f, 1f, 0.5f))
        assertNear(0.5f, smoothStep(10f, 20f, 15f))
        assertNear(0.15625f, smoothStep(0f, 1f, 0.25f))
    }

    @Test
    fun test_tri_area() {
        assertNear(0.5f, triArea(Vec3f(0f, 0f, 0f), Vec3f(1f, 0f, 0f), Vec3f(0f, 1f, 0f)))
        assertNear(6f, triArea(Vec3f(0f, 0f, 0f), Vec3f(0f, 3f, 0f), Vec3f(0f, 0f, 4f)))
        assertNear(0f, triArea(Vec3f(0f, 0f, 0f), Vec3f(1f, 1f, 1f), Vec3f(2f, 2f, 2f)))
    }

    @Test
    fun test_tri_aspect_ratio_equilateral_is_one() {
        val h = sqrt(3f) / 2f
        assertNear(
            1f,
            triAspectRatio(Vec3f(0f, 0f, 0f), Vec3f(1f, 0f, 0f), Vec3f(0.5f, h, 0f)),
            1e-3f
        )
    }

    @Test
    fun test_barycentric_weights_at_vertices_and_centroid() {
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(4f, 0f, 0f)
        val c = Vec3f(0f, 4f, 0f)
        val result = MutableVec3f()

        barycentricWeights(a, a, b, c, result)
        assertTrue(result.isFuzzyEqual(Vec3f(1f, 0f, 0f)))
        barycentricWeights(b, a, b, c, result)
        assertTrue(result.isFuzzyEqual(Vec3f(0f, 1f, 0f)))
        barycentricWeights(c, a, b, c, result)
        assertTrue(result.isFuzzyEqual(Vec3f(0f, 0f, 1f)))

        val centroid = Vec3f(4f / 3f, 4f / 3f, 0f)
        barycentricWeights(centroid, a, b, c, result)
        assertNear(1f / 3f, result.x)
        assertNear(1f / 3f, result.y)
        assertNear(1f / 3f, result.z)
    }

    @Test
    fun test_barycentric_weights_sum_to_one() {
        val result =
            barycentricWeights(
                Vec3f(1f, 0.5f, 0f),
                Vec3f(0f, 0f, 0f),
                Vec3f(3f, 0f, 0f),
                Vec3f(0f, 2f, 0f),
                MutableVec3f()
            )
        assertNear(1f, result.x + result.y + result.z)
    }

    @Test
    fun test_distance_to_line() {
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(1f, 0f, 0f)
        assertNear(2f, Vec3f(5f, 2f, 0f).distanceToLine(a, b))
        assertNear(2f, Vec3f(-5f, 0f, 2f).distanceToLine(a, b))
        assertNear(0f, Vec3f(7f, 0f, 0f).distanceToLine(a, b))
    }

    @Test
    fun test_nearest_point_on_line() {
        val result = MutableVec3f()
        Vec3f(5f, 2f, 3f).nearestPointOnLine(Vec3f(0f, 1f, 0f), Vec3f(2f, 1f, 0f), result)
        assertTrue(result.isFuzzyEqual(Vec3f(5f, 1f, 0f)))
    }

    @Test
    fun test_distance_to_ray_in_front_and_behind() {
        val origin = Vec3f(0f, 0f, 0f)
        val dir = Vec3f(0f, 0f, 1f)
        assertNear(3f, Vec3f(3f, 0f, 10f).distanceToRay(origin, dir))
        assertNear(5f, Vec3f(3f, 0f, -4f).distanceToRay(origin, dir))
    }

    @Test
    fun test_nearest_point_on_ray() {
        val result = MutableVec3f()
        Vec3f(3f, 0f, 10f).nearestPointOnRay(Vec3f.ZERO, Vec3f(0f, 0f, 2f), result)
        assertTrue(result.isFuzzyEqual(Vec3f(0f, 0f, 10f)))
        Vec3f(3f, 0f, -10f).nearestPointOnRay(Vec3f.ZERO, Vec3f(0f, 0f, 2f), result)
        assertTrue(result.isFuzzyEqual(Vec3f.ZERO))
    }

    @Test
    fun test_distance_to_edge_2d() {
        val a = Vec2f(0f, 0f)
        val b = Vec2f(4f, 0f)
        assertNear(1f, Vec2f(2f, 1f).distanceToEdge(a, b))
        assertNear(5f, Vec2f(-3f, 4f).distanceToEdge(a, b))
        assertNear(5f, Vec2f(7f, -4f).distanceToEdge(a, b))
    }

    @Test
    fun test_nearest_point_on_edge_2d() {
        val a = Vec2f(0f, 0f)
        val b = Vec2f(4f, 0f)
        val result = MutableVec2f()
        assertTrue(Vec2f(2f, 3f).nearestPointOnEdge(a, b, result).isFuzzyEqual(Vec2f(2f, 0f)))
        assertTrue(Vec2f(-2f, 3f).nearestPointOnEdge(a, b, result).isFuzzyEqual(a))
        assertTrue(Vec2f(9f, 3f).nearestPointOnEdge(a, b, result).isFuzzyEqual(b))
    }

    @Test
    fun test_distance_to_edge_3d() {
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(0f, 0f, 4f)
        assertNear(1f, Vec3f(1f, 0f, 2f).distanceToEdge(a, b))
        assertNear(5f, Vec3f(0f, 3f, 8f).distanceToEdge(a, b))
        assertNear(2f, Vec3f(0f, 0f, -2f).distanceToEdge(a, b))
    }

    @Test
    fun test_nearest_point_on_edge_3d() {
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(0f, 0f, 4f)
        val result = MutableVec3f()
        assertTrue(Vec3f(1f, 1f, 3f).nearestPointOnEdge(a, b, result).isFuzzyEqual(Vec3f(0f, 0f, 3f)))
        assertTrue(Vec3f(1f, 1f, 6f).nearestPointOnEdge(a, b, result).isFuzzyEqual(b))
        assertTrue(Vec3f(1f, 1f, -6f).nearestPointOnEdge(a, b, result).isFuzzyEqual(a))
    }

    @Test
    fun test_map() {
        assertNear(50f, map(0f, 10f, 0f, 100f, 5f))
        assertNear(-1f, map(0f, 1f, -1f, 1f, 0f))
        assertNear(150f, map(0f, 10f, 0f, 100f, 15f))
        assertNear(75f, map(10f, 0f, 0f, 100f, 2.5f))
    }

    @Test
    fun test_next_power_of_two() {
        assertEquals(1, 1.nextPowerOfTwo)
        assertEquals(2, 2.nextPowerOfTwo)
        assertEquals(4, 3.nextPowerOfTwo)
        assertEquals(4, 4.nextPowerOfTwo)
        assertEquals(8, 5.nextPowerOfTwo)
        assertEquals(1024, 1023.nextPowerOfTwo)
        assertEquals(1024, 1024.nextPowerOfTwo)
        assertEquals(2048, 1025.nextPowerOfTwo)
    }

    @Test
    fun test_prev_power_of_two() {
        assertEquals(1, 1.prevPowerOfTwo)
        assertEquals(4, 5.prevPowerOfTwo)
        assertEquals(8, 8.prevPowerOfTwo)
        assertEquals(8, 9.prevPowerOfTwo)
        assertEquals(512, 1023.prevPowerOfTwo)
    }

    @Test
    fun test_is_power_of_two() {
        assertTrue(1.isPowerOfTwo)
        assertTrue(2.isPowerOfTwo)
        assertTrue(64.isPowerOfTwo)
        assertFalse(3.isPowerOfTwo)
        assertFalse(100.isPowerOfTwo)
        assertFalse(0.isPowerOfTwo)
        assertFalse((-4).isPowerOfTwo)
    }

    @Test
    fun test_interpolate() {
        assertNear(5f, 0.5f.interpolate(0f, 10f))
        assertEquals(5, 0.5f.interpolate(0, 10))
        assertEquals(50L, 0.5f.interpolate(0L, 100L))
        assertNear(2.5, 0.25f.interpolate(0.0, 10.0))
        assertNear(7.5f, 0.75.interpolate(0f, 10f))
        assertNear(-5.0, 0.5.interpolate(0.0, -10.0))
        assertEquals(15, 0.5.interpolate(10, 20))
        assertEquals(15L, 0.5.interpolate(10L, 20L))
        assertNear(10f, 0f.interpolate(10f, 20f))
        assertNear(20f, 1f.interpolate(10f, 20f))
    }

    @Test
    fun test_random_ranges_stay_in_bounds() {
        repeat(100) {
            val f = (2f..5f).random()
            assertTrue(f in 2f..5f)
            val d = (-1.0..1.0).random()
            assertTrue(d in -1.0..1.0)
            val i = (3..7).random()
            assertTrue(i in 3f..7f)
        }
    }

    @Test
    fun test_sparse_list_of() {
        assertEquals(listOf(1, 2, 3, 7, 8), sparseListOf(1..3, 7..8))
    }

    @Test
    fun test_dist_and_dist_sqr() {
        assertNear(25f, distSqr(0f, 0f, 3f, 4f))
        assertNear(5f, dist(0f, 0f, 3f, 4f))
        assertNear(25.0, distSqr(1.0, 1.0, 4.0, 5.0))
        assertNear(5.0, dist(1.0, 1.0, 4.0, 5.0))
        assertNear(25.0, distSqr(0, 0, -3, -4))
        assertNear(5.0, dist(0, 0, -3, -4))
    }

    @Test
    fun test_normalize_radian() {
        assertNear(0.0, normalizeRadian(0.0))
        assertNear(PI, normalizeRadian(3 * PI))
        assertNear(-PI / 2, normalizeRadian(3 * PI / 2))
        assertNear(PI / 2, normalizeRadian(-3 * PI / 2))
        assertNear(1.0, normalizeRadian(1.0 + 4 * PI), 1e-9)
    }

    @Test
    fun test_subtract_and_dist_radians_wrap_around() {
        assertNear(0.2, subtractRadians(0.1, -0.1))
        assertNear(-0.2, subtractRadians(PI * 2 - 0.1, 0.1), 1e-9)
        assertNear(0.2, distRadians(0.1, PI * 2 - 0.1), 1e-9)
        assertNear(PI, distRadians(0.0, PI))
        assertNear(0.0, distRadians(0, 0))
    }
}
