package com.littlekt.math.geom

import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AngleTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNearDegrees(expected: Float, actual: Angle, eps: Float = 1e-3f) {
        assertNear(expected, actual.degrees, eps)
    }

    @Test
    fun test_degrees_to_radians() {
        assertNear(PI_F, 180.degrees.radians)
        assertNear(PI_F / 2f, 90f.degrees.radians)
        assertNear(PI_F / 4f, 45.0.degrees.radians)
        assertNear(-PI_F, (-180).degrees.radians)
    }

    @Test
    fun test_radians_to_degrees() {
        assertNear(180f, PI_F.radians.degrees, 1e-3f)
        assertNear(90f, (PI_F / 2f).radians.degrees, 1e-3f)
        assertNear(57.29578f, 1.radians.degrees, 1e-3f)
        assertNear(360f, PI2_F.toDouble().radians.degrees, 1e-3f)
    }

    @Test
    fun test_from_factories() {
        assertEquals(Angle(1.5f), Angle.fromRadians(1.5f))
        assertNear(PI_F, Angle.fromDegrees(180f).radians)
        assertNear(PI_F, Angle.fromDegrees(180.0).radians)
        assertNear(2f, Angle.fromRadians(2).radians)
        assertEquals(0f, Angle.ZERO.radians)
    }

    @Test
    fun test_round_trip_degrees() {
        for (d in -720..720 step 15) {
            assertNear(d.toFloat(), d.degrees.degrees, 1e-3f)
        }
    }

    @Test
    fun test_trig_functions() {
        assertNear(1f, sin(90.degrees))
        assertNear(0f, cos(90.degrees))
        assertNear(1f, tan(45.degrees))
        assertNear(-1f, 180.degrees.cosine)
        assertNear(sqrt(3f) / 2f, 60.degrees.sine)
        assertNear(1f, 45.degrees.tangent)
    }

    @Test
    fun test_arithmetic() {
        assertNearDegrees(90f, 30.degrees + 60.degrees)
        assertNearDegrees(-30f, 30.degrees - 60.degrees)
        assertNearDegrees(-45f, -(45.degrees))
        assertNearDegrees(45f, +(45.degrees))
        assertNearDegrees(90f, 45.degrees * 2)
        assertNearDegrees(90f, 45.degrees * 2f)
        assertNearDegrees(90f, 45.degrees * 2.0)
        assertNearDegrees(15f, 45.degrees / 3)
        assertNearDegrees(15f, 45.degrees / 3f)
        assertNearDegrees(15f, 45.degrees / 3.0)
        assertNearDegrees(90f, 2 * 45.degrees)
        assertNearDegrees(90f, 2f * 45.degrees)
        assertNearDegrees(90f, 2.0 * 45.degrees)
        assertNear(3f, 90.degrees / 30.degrees)
    }

    @Test
    fun test_comparison() {
        assertTrue(10.degrees < 20.degrees)
        assertTrue(20.degrees > 10.degrees)
        assertTrue((-10).degrees < Angle.ZERO)
        assertEquals(0, 10.degrees.compareTo(10.degrees))
    }

    @Test
    fun test_absolute_value() {
        assertNearDegrees(30f, abs((-30).degrees))
        assertNearDegrees(30f, (-30).degrees.absoluteValue)
        assertNearDegrees(30f, 30.degrees.absoluteValue)
    }

    @Test
    fun test_normalized() {
        assertNearDegrees(10f, 370.degrees.normalized)
        assertNearDegrees(350f, (-10).degrees.normalized)
        assertNearDegrees(0f, 720.degrees.normalized)
        assertNearDegrees(90f, 90.degrees.normalized)
        assertNearDegrees(270f, (-450).degrees.normalized)
    }

    @Test
    fun test_short_distance() {
        assertNearDegrees(20f, 10.degrees.shortDistanceTo(30.degrees))
        assertNearDegrees(-20f, 30.degrees.shortDistanceTo(10.degrees))
        assertNearDegrees(-20f, 10.degrees.shortDistanceTo(350.degrees))
        assertNearDegrees(20f, 350.degrees.shortDistanceTo(10.degrees))
        assertNearDegrees(0f, 45.degrees.shortDistanceTo(405.degrees))
        assertNearDegrees(-90f, 0.degrees.shortDistanceTo((-90).degrees))
    }

    @Test
    fun test_short_distance_is_within_half_turn() {
        for (from in -360..360 step 37) {
            for (to in -360..360 step 41) {
                val d = from.degrees.shortDistanceTo(to.degrees)
                assertTrue(abs(d.degrees) <= 180.001f, "from=$from to=$to d=${d.degrees}")
                assertNearDegrees(0f, (from.degrees + d).shortDistanceTo(to.degrees), 1e-2f)
            }
        }
    }

    @Test
    fun test_long_distance() {
        assertNearDegrees(-340f, 10.degrees.longDistanceTo(30.degrees))
        assertNearDegrees(340f, 10.degrees.longDistanceTo(350.degrees))
        assertNearDegrees(0f, 10.degrees.longDistanceTo(10.degrees))
    }

    @Test
    fun test_between() {
        assertNearDegrees(0f, Angle.between(0f, 0f, 1f, 0f))
        assertNearDegrees(90f, Angle.between(0f, 0f, 0f, 1f))
        assertNearDegrees(180f, Angle.between(0f, 0f, -1f, 0f))
        assertNearDegrees(270f, Angle.between(0f, 0f, 0f, -1f))
        assertNearDegrees(45f, Angle.between(1, 1, 2, 2))
        assertNearDegrees(315f, Angle.between(1.0, 1.0, 2.0, 0.0))
    }

    @Test
    fun test_in_between() {
        assertTrue(45.degrees.inBetweenInclusive(0.degrees, 90.degrees))
        assertTrue(90.degrees.inBetweenInclusive(0.degrees, 90.degrees))
        assertFalse(90.degrees.inBetweenExclusive(0.degrees, 90.degrees))
        assertTrue(0.degrees.inBetweenExclusive(0.degrees, 90.degrees))
        assertFalse(135.degrees.inBetweenInclusive(0.degrees, 90.degrees))
        assertTrue(405.degrees.inBetweenInclusive(0.degrees, 90.degrees))
    }

    @Test
    fun test_in_between_wraps_around_zero() {
        assertTrue(0.degrees.inBetweenInclusive(350.degrees, 10.degrees))
        assertTrue(355.degrees.inBetweenInclusive(350.degrees, 10.degrees))
        assertTrue((-5).degrees.inBetweenInclusive(350.degrees, 10.degrees))
        assertFalse(180.degrees.inBetweenInclusive(350.degrees, 10.degrees))
    }

    @Test
    fun test_in_between_ranges() {
        assertTrue(45.degrees inBetween 0.degrees..90.degrees)
        assertTrue(90.degrees inBetween 0.degrees..90.degrees)
        assertFalse(90.degrees inBetween (0.degrees until 90.degrees))
        assertTrue(89.degrees inBetween (0.degrees until 90.degrees))
    }

    @Test
    fun test_open_range_contains() {
        val range = 0.degrees until 90.degrees
        assertTrue(0.degrees in range)
        assertTrue(45.degrees in range)
        assertFalse(90.degrees in range)
        assertFalse((-1).degrees in range)
        assertTrue(1 in OpenRange(1, 3))
        assertFalse(3 in OpenRange(1, 3))
    }

    @Test
    fun test_interpolate() {
        assertNearDegrees(45f, 0.5.interpolate(0.degrees, 90.degrees))
        assertNearDegrees(0f, 0.0.interpolate(0.degrees, 90.degrees))
        assertNearDegrees(90f, 1.0.interpolate(0.degrees, 90.degrees))
    }

    @Test
    fun test_ratio_helpers() {
        assertNear(1f, Angle.cos01(0.0).toFloat())
        assertNear(1f, Angle.sin01(0.25).toFloat())
        assertNear(1f, Angle.tan01(0.125).toFloat())
    }
}
