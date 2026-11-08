package com.littlekt.math.spatial

import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BoundingSphereTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_default_is_zero() {
        val sphere = BoundingSphere()
        assertEquals(Vec3f.ZERO, sphere.center)
        assertEquals(0f, sphere.radius)
    }

    @Test
    fun test_update_sets_center_and_radius() {
        val sphere = BoundingSphere().update(Vec3f(1f, 2f, 3f), 4f)
        assertEquals(Vec3f(1f, 2f, 3f), sphere.center)
        assertEquals(4f, sphere.radius)
    }

    @Test
    fun test_update_keeps_radius_by_default() {
        val sphere = BoundingSphere().update(Vec3f.ZERO, 4f)
        sphere.update(Vec3f(1f, 1f, 1f))
        assertEquals(Vec3f(1f, 1f, 1f), sphere.center)
        assertEquals(4f, sphere.radius)
    }

    @Test
    fun test_add_point_grows_radius() {
        val sphere = BoundingSphere().update(Vec3f(1f, 0f, 0f), 1f)
        sphere.addPoint(Vec3f(1f, 3f, 4f))
        assertNear(5f, sphere.radius)
        assertEquals(Vec3f(1f, 0f, 0f), sphere.center)
    }

    @Test
    fun test_add_point_inside_keeps_radius() {
        val sphere = BoundingSphere().update(Vec3f.ZERO, 10f)
        sphere.addPoint(Vec3f(1f, 2f, 3f))
        assertEquals(10f, sphere.radius)
    }

    @Test
    fun test_clear() {
        val sphere = BoundingSphere().update(Vec3f(1f, 2f, 3f), 4f)
        sphere.clear()
        assertEquals(Vec3f.ZERO, sphere.center)
        assertEquals(0f, sphere.radius)
    }

    @Test
    fun test_equals_and_hash_code() {
        val a = BoundingSphere().update(Vec3f(1f, 2f, 3f), 4f)
        val b = BoundingSphere().update(Vec3f(1f, 2f, 3f), 4f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, BoundingSphere().update(Vec3f(1f, 2f, 3f), 5f))
        assertNotEquals(a, BoundingSphere().update(Vec3f(1f, 2f, 0f), 4f))
    }
}
