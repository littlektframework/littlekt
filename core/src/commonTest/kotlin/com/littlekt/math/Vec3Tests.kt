package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Vec3Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    @Test
    fun test_cross() {
        assertEquals(Vec3f(0f, 0f, 1f), Vec3f.X_AXIS.cross(Vec3f.Y_AXIS, MutableVec3f()))
    }

    @Test
    fun test_cross_in_place() {
        assertEquals(Vec3f(0f, 0f, 1f), MutableVec3f(1f, 0f, 0f).cross(Vec3f.Y_AXIS))
        assertEquals(Vec3f(-3f, 6f, -3f), MutableVec3f(1f, 2f, 3f).cross(Vec3f(4f, 5f, 6f)))
    }

    @Test
    fun test_cross_into_other() {
        val other = MutableVec3f(4f, 5f, 6f)
        Vec3f(1f, 2f, 3f).cross(other, other)
        assertEquals(Vec3f(-3f, 6f, -3f), other)
    }

    @Test
    fun test_cross_is_anticommutative_and_orthogonal() {
        val a = Vec3f(1f, 2f, 3f)
        val b = Vec3f(-2f, 0.5f, 4f)
        val ab = a.cross(b, MutableVec3f())
        val ba = b.cross(a, MutableVec3f())
        assertNear(ab, ba.scale(-1f))
        assertNear(0f, ab.dot(a))
        assertNear(0f, ab.dot(b))
    }

    @Test
    fun test_cross_axes() {
        assertEquals(Vec3f.X_AXIS, Vec3f.Y_AXIS.cross(Vec3f.Z_AXIS, MutableVec3f()))
        assertEquals(Vec3f.Y_AXIS, Vec3f.Z_AXIS.cross(Vec3f.X_AXIS, MutableVec3f()))
        assertEquals(Vec3f.ZERO, Vec3f(1f, 2f, 3f).cross(Vec3f(2f, 4f, 6f), MutableVec3f()))
    }

    @Test
    fun test_add_subtract() {
        assertEquals(Vec3f(5f, 7f, 9f), Vec3f(1f, 2f, 3f).add(Vec3f(4f, 5f, 6f), MutableVec3f()))
        assertEquals(
            Vec3f(-3f, -3f, -3f),
            Vec3f(1f, 2f, 3f).subtract(Vec3f(4f, 5f, 6f), MutableVec3f()),
        )
        assertEquals(Vec3f(2f, 3f, 4f), MutableVec3f(1f, 1f, 1f).add(1f, 2f, 3f))
    }

    @Test
    fun test_scale_and_mul() {
        assertEquals(Vec3f(2f, 4f, 6f), Vec3f(1f, 2f, 3f).scale(2f, MutableVec3f()))
        assertEquals(Vec3f(4f, 10f, 18f), Vec3f(1f, 2f, 3f).mul(Vec3f(4f, 5f, 6f), MutableVec3f()))
        assertEquals(Vec3f(2f, 6f, 12f), MutableVec3f(1f, 2f, 3f).scale(2f, 3f, 4f))
    }

    @Test
    fun test_operators() {
        val v = MutableVec3f(2f, 4f, 6f)
        v += Vec3f(1f)
        assertEquals(Vec3f(3f, 5f, 7f), v)
        v -= Vec3f(1f)
        assertEquals(Vec3f(2f, 4f, 6f), v)
        v *= 0.5f
        assertEquals(Vec3f(1f, 2f, 3f), v)
        v /= Vec3f(1f, 2f, 3f)
        assertEquals(Vec3f.ONES, v)
        v /= 2f
        assertEquals(Vec3f(0.5f), v)
        assertEquals(32f, Vec3f(1f, 2f, 3f) * Vec3f(4f, 5f, 6f))
    }

    @Test
    fun test_length_and_distance() {
        assertEquals(3f, Vec3f(1f, 2f, 2f).length())
        assertEquals(9f, Vec3f(1f, 2f, 2f).sqrLength())
        assertEquals(3f, Vec3f(1f, 1f, 1f).distance(Vec3f(2f, 3f, 3f)))
        assertEquals(9f, Vec3f(1f, 1f, 1f).sqrDistance(Vec3f(2f, 3f, 3f)))
    }

    @Test
    fun test_dot() {
        assertEquals(32f, Vec3f(1f, 2f, 3f).dot(Vec3f(4f, 5f, 6f)))
        assertEquals(0f, Vec3f.X_AXIS.dot(Vec3f.Z_AXIS))
    }

    @Test
    fun test_norm() {
        val n = Vec3f(1f, 2f, 2f).norm(MutableVec3f())
        assertNear(1f, n.length())
        assertNear(Vec3f(1f / 3f, 2f / 3f, 2f / 3f), n)
    }

    @Test
    fun test_norm_zero_stays_zero() {
        assertEquals(Vec3f.ZERO, MutableVec3f().norm())
    }

    @Test
    fun test_mix_and_lerp() {
        val a = Vec3f(0f, 10f, -10f)
        val b = Vec3f(10f, 20f, 10f)
        assertEquals(a, a.mix(b, 0f, MutableVec3f()))
        assertEquals(b, a.mix(b, 1f, MutableVec3f()))
        assertEquals(Vec3f(5f, 15f, 0f), a.lerp(b, 0.5f, MutableVec3f()))
        assertEquals(Vec3f(2.5f, 12.5f, -5f), MutableVec3f(a).lerp(b, 0.25f))
    }

    @Test
    fun test_rotate_90_about_z_maps_x_to_y() {
        assertNear(Vec3f.Y_AXIS, MutableVec3f(Vec3f.X_AXIS).rotate(90.degrees, Vec3f.Z_AXIS))
    }

    @Test
    fun test_rotate_90_about_x_maps_y_to_z() {
        assertNear(Vec3f.Z_AXIS, Vec3f.Y_AXIS.rotate(90.degrees, Vec3f.X_AXIS, MutableVec3f()))
    }

    @Test
    fun test_rotate_90_about_y_maps_z_to_x() {
        assertNear(Vec3f.X_AXIS, Vec3f.Z_AXIS.rotate(90.degrees, 0f, 1f, 0f, MutableVec3f()))
    }

    @Test
    fun test_rotate_agrees_with_mat4_rotation() {
        val axis = Vec3f(1f, 2f, 3f).norm(MutableVec3f())
        val v = Vec3f(0.3f, -1.2f, 2f)
        val expected = Mat4().setToRotation(axis, 50.degrees).transform(MutableVec3f(v), 0f)
        assertNear(expected, v.rotate(50.degrees, axis, MutableVec3f()))
    }

    @Test
    fun test_rotate_in_place_result() {
        val v = MutableVec3f(1f, 0f, 0f)
        v.rotate(90.degrees, Vec3f.Z_AXIS, v)
        assertNear(Vec3f.Y_AXIS, v)
    }

    @Test
    fun test_mul_mat4_applies_translation() {
        val m = Mat4().setToTranslate(1f, 2f, 3f).scale(2f)
        assertEquals(Vec3f(3f, 4f, 5f), MutableVec3f(1f, 1f, 1f).mul(m))
    }

    @Test
    fun test_plane_space_is_orthonormal() {
        val normals = listOf(Vec3f.Z_AXIS, Vec3f.X_AXIS, Vec3f(1f, 2f, 3f).norm(MutableVec3f()))
        for (n in normals) {
            val p = MutableVec3f()
            val q = MutableVec3f()
            n.planeSpace(p, q)
            assertNear(1f, p.length())
            assertNear(1f, q.length())
            assertNear(0f, p.dot(n))
            assertNear(0f, q.dot(n))
            assertNear(0f, p.dot(q))
            assertNear(q, n.cross(p, MutableVec3f()))
        }
    }

    @Test
    fun test_is_fuzzy_equal() {
        assertTrue(Vec3f(1f, 2f, 3f).isFuzzyEqual(Vec3f(1f, 2f, 3f + 1e-6f)))
        assertFalse(Vec3f(1f, 2f, 3f).isFuzzyEqual(Vec3f(1f, 2f, 3.1f)))
        assertTrue(Vec3f(1f, 2f, 3f).isFuzzyEqual(Vec3f(1f, 2f, 3.1f), 0.2f))
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(Vec3f(1f, 2f, 3f), MutableVec3f(1f, 2f, 3f))
        assertEquals(Vec3f(1f, 2f, 3f).hashCode(), MutableVec3f(1f, 2f, 3f).hashCode())
        assertNotEquals(Vec3f(1f, 2f, 3f), Vec3f(1f, 2f, 4f))
    }

    @Test
    fun test_conversions_copy() {
        val v = MutableVec3f(1f, 2f, 3f)
        val copy = v.toMutableVec3()
        copy.z = 9f
        assertEquals(3f, v.z)
        assertEquals(Vec3f(1f, 2f, 3f), v.toVec3())
    }

    @Test
    fun test_result_aliasing_this() {
        val v = MutableVec3f(1f, 2f, 3f)
        v.add(Vec3f(1f), v)
        assertEquals(Vec3f(2f, 3f, 4f), v)
        v.mix(Vec3f.ZERO, 0.5f, v)
        assertEquals(Vec3f(1f, 1.5f, 2f), v)
        v.scale(2f, v)
        assertEquals(Vec3f(2f, 3f, 4f), v)
        v.norm(v)
        assertNear(1f, v.length())
    }

    @Test
    fun test_result_aliasing_other() {
        val other = MutableVec3f(4f, 5f, 6f)
        Vec3f(1f, 2f, 3f).subtract(other, other)
        assertEquals(Vec3f(-3f, -3f, -3f), other)
        val o2 = MutableVec3f(10f, 20f, 30f)
        Vec3f.ZERO.mix(o2, 0.5f, o2)
        assertEquals(Vec3f(5f, 10f, 15f), o2)
    }

    @Test
    fun test_vec3i() {
        val v = MutableVec3i(1, 2, 3)
        v.add(Vec3i(1, 1, 1))
        assertEquals(Vec3i(2, 3, 4), v)
        v.add(1, 2, 3)
        assertEquals(Vec3i(3, 5, 7), v)
        v.subtract(Vec3i(3, 5, 7))
        assertEquals(Vec3i.ZERO, v)
        v.set(4, 5, 6)
        v[2] = 9
        assertEquals(Vec3i(4, 5, 9), v)
        assertEquals(9, v.z)
        assertEquals(Vec3i(1, 2, 3).hashCode(), MutableVec3i(1, 2, 3).hashCode())
        assertNotEquals(Vec3i(1, 2, 3), Vec3i(3, 2, 1))
        assertEquals(Vec3i(7, 7, 7), MutableVec3i().set(Vec3i(7)))
    }
}
