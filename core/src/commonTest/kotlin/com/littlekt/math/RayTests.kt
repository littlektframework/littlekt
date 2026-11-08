package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RayTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-4f) {
        assertTrue(actual.isFuzzyEqual(expected, eps), "expected $expected but was $actual")
    }

    private fun ray(origin: Vec3f, direction: Vec3f) =
        Ray().apply {
            this.origin.set(origin)
            this.direction.set(direction)
        }

    @Test
    fun test_set_copies_other_ray() {
        val a = ray(Vec3f(1f, 2f, 3f), Vec3f(0f, 1f, 0f))
        val b = Ray()
        b.set(a)
        assertNear(a.origin, b.origin)
        assertNear(a.direction, b.direction)
        a.origin.set(9f, 9f, 9f)
        assertNear(Vec3f(1f, 2f, 3f), b.origin)
    }

    @Test
    fun test_set_from_look_at_normalizes_direction() {
        val r = Ray()
        r.setFromLookAt(Vec3f(1f, 1f, 1f), Vec3f(1f, 1f, 11f))
        assertNear(Vec3f(1f, 1f, 1f), r.origin)
        assertNear(Vec3f(0f, 0f, 1f), r.direction)
    }

    @Test
    fun test_distance_to_point() {
        val r = ray(Vec3f.ZERO, Vec3f(1f, 0f, 0f))
        assertNear(3f, r.distanceToPoint(Vec3f(5f, 3f, 0f)))
        assertNear(9f, r.sqrDistanceToPoint(Vec3f(5f, 0f, 3f)))
        assertNear(16f, r.sqrDistanceToPoint(2f, 0f, 4f))
    }

    @Test
    fun test_distance_to_point_behind_origin_uses_origin() {
        val r = ray(Vec3f.ZERO, Vec3f(1f, 0f, 0f))
        assertNear(5f, r.distanceToPoint(Vec3f(-4f, 3f, 0f)))
    }

    @Test
    fun test_sphere_intersection_hits_near_side() {
        val r = ray(Vec3f(0f, 0f, -10f), Vec3f(0f, 0f, 1f))
        val result = MutableVec3f()
        assertTrue(r.sphereIntersection(Vec3f.ZERO, 1f, result))
        assertNear(Vec3f(0f, 0f, -1f), result)
    }

    @Test
    fun test_sphere_intersection_with_offset_center() {
        val r = ray(Vec3f(5f, 0f, 0f), Vec3f(-1f, 0f, 0f))
        val result = MutableVec3f()
        assertTrue(r.sphereIntersection(Vec3f(0f, 1f, 0f), 2f, result))
        assertNear(Vec3f(sqrt(3f), 0f, 0f), result)
    }

    @Test
    fun test_sphere_intersection_with_unnormalized_direction() {
        val r = ray(Vec3f(0f, 0f, -10f), Vec3f(0f, 0f, 4f))
        val result = MutableVec3f()
        assertTrue(r.sphereIntersection(Vec3f.ZERO, 2f, result))
        assertNear(Vec3f(0f, 0f, -2f), result)
    }

    @Test
    fun test_sphere_intersection_from_inside_hits_far_side() {
        val r = ray(Vec3f.ZERO, Vec3f(0f, 1f, 0f))
        val result = MutableVec3f()
        assertTrue(r.sphereIntersection(Vec3f.ZERO, 3f, result))
        assertNear(Vec3f(0f, 3f, 0f), result)
    }

    @Test
    fun test_sphere_intersection_miss() {
        val r = ray(Vec3f(0f, 5f, -10f), Vec3f(0f, 0f, 1f))
        assertFalse(r.sphereIntersection(Vec3f.ZERO, 1f, MutableVec3f()))
    }

    @Test
    fun test_sphere_intersection_behind_origin_misses() {
        val r = ray(Vec3f(0f, 0f, 10f), Vec3f(0f, 0f, 1f))
        assertFalse(r.sphereIntersection(Vec3f.ZERO, 1f, MutableVec3f()))
    }

    @Test
    fun test_sphere_intersection_tangent() {
        val r = ray(Vec3f(1f, 0f, -10f), Vec3f(0f, 0f, 1f))
        val result = MutableVec3f()
        assertTrue(r.sphereIntersection(Vec3f.ZERO, 1f, result))
        assertNear(Vec3f(1f, 0f, 0f), result, 1e-3f)
    }

    @Test
    fun test_transform_by_translation_moves_origin_only() {
        val r = ray(Vec3f(1f, 2f, 3f), Vec3f(0f, 0f, 1f))
        r.transformBy(Mat4().setToTranslate(10f, 0f, -5f))
        assertNear(Vec3f(11f, 2f, -2f), r.origin)
        assertNear(Vec3f(0f, 0f, 1f), r.direction)
    }

    @Test
    fun test_transform_by_rotation_and_scale_keeps_direction_normalized() {
        val r = ray(Vec3f(1f, 0f, 0f), Vec3f(1f, 0f, 0f))
        val m = Mat4().rotate(Vec3f.Z_AXIS, 90.degrees).scale(2f, 2f, 2f)
        r.transformBy(m)
        assertNear(Vec3f(0f, 2f, 0f), r.origin)
        assertNear(Vec3f(0f, 1f, 0f), r.direction)
    }
}
