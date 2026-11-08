package com.littlekt.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Vec4Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec4f, actual: Vec4f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    @Test
    fun test_constructors() {
        assertEquals(Vec4f(2f, 2f, 2f, 2f), Vec4f(2f))
        assertEquals(Vec4f(1f, 2f, 3f, 4f), Vec4f(Vec3f(1f, 2f, 3f), 4f))
        assertEquals(Vec4f(1f, 2f, 3f, 4f), MutableVec4f(Vec4f(1f, 2f, 3f, 4f)))
        assertEquals(Vec4f.ZERO, MutableVec4f())
    }

    @Test
    fun test_add_subtract() {
        val a = Vec4f(1f, 2f, 3f, 4f)
        val b = Vec4f(4f, 3f, 2f, 1f)
        assertEquals(Vec4f(5f), a.add(b, MutableVec4f()))
        assertEquals(Vec4f(-3f, -1f, 1f, 3f), a.subtract(b, MutableVec4f()))
        val v = MutableVec4f(a)
        v += b
        assertEquals(Vec4f(5f), v)
        v -= b
        assertEquals(a, v)
    }

    @Test
    fun test_scale_and_mul() {
        assertEquals(Vec4f(2f, 4f, 6f, 8f), Vec4f(1f, 2f, 3f, 4f).scale(2f, MutableVec4f()))
        assertEquals(
            Vec4f(4f, 6f, 6f, 4f),
            Vec4f(1f, 2f, 3f, 4f).mul(Vec4f(4f, 3f, 2f, 1f), MutableVec4f()),
        )
    }

    @Test
    fun test_dot_and_times() {
        assertEquals(20f, Vec4f(1f, 2f, 3f, 4f).dot(Vec4f(4f, 3f, 2f, 1f)))
        assertEquals(20f, Vec4f(1f, 2f, 3f, 4f) * Vec4f(4f, 3f, 2f, 1f))
    }

    @Test
    fun test_length() {
        assertEquals(30f, Vec4f(1f, 2f, 3f, 4f).sqrLength())
        assertEquals(2f, Vec4f(1f).length())
    }

    @Test
    fun test_distance_uses_w() {
        assertEquals(4f, Vec4f(0f, 0f, 0f, 0f).sqrDistance(Vec4f(0f, 0f, 0f, 2f)))
        assertEquals(2f, Vec4f(0f, 0f, 5f, 1f).distance(Vec4f(0f, 0f, 5f, 3f)))
        assertEquals(30f, Vec4f(1f, 2f, 3f, 4f).sqrDistance(Vec4f.ZERO))
    }

    @Test
    fun test_norm() {
        val n = Vec4f(1f, 1f, 1f, 1f).norm(MutableVec4f())
        assertNear(Vec4f(0.5f), n)
        assertNear(1f, n.length())
    }

    @Test
    fun test_norm_zero_stays_zero() {
        assertEquals(Vec4f.ZERO, MutableVec4f().norm())
    }

    @Test
    fun test_set_xyz_and_get_xyz() {
        val v = MutableVec4f(9f, 9f, 9f, 9f)
        assertEquals(Vec4f(1f, 2f, 3f, 0f), v.set(Vec3f(1f, 2f, 3f)))
        assertEquals(Vec4f(1f, 2f, 3f, 1f), v.set(Vec3f(1f, 2f, 3f), 1f))
        assertEquals(Vec3f(1f, 2f, 3f), Vec4f(1f, 2f, 3f, 4f).getXyz(MutableVec3f()))
    }

    @Test
    fun test_set_index() {
        val v = MutableVec4f()
        v[3] = 7f
        assertEquals(7f, v.w)
        v.w = 2f
        assertEquals(2f, v[3])
    }

    @Test
    fun test_to_quaternion() {
        val q = Vec4f(1f, 2f, 3f, 4f).toMutableQuaternion()
        assertEquals(1f, q.x)
        assertEquals(2f, q.y)
        assertEquals(3f, q.z)
        assertEquals(4f, q.w)
        assertEquals(4f, Vec4f(1f, 2f, 3f, 4f).toQuaternion().w)
    }

    @Test
    fun test_conversions_copy() {
        val v = MutableVec4f(1f, 2f, 3f, 4f)
        val copy = v.toMutableVec4()
        copy.w = 9f
        assertEquals(4f, v.w)
        assertEquals(Vec4f(1f, 2f, 3f, 4f), v.toVec4())
    }

    @Test
    fun test_is_fuzzy_equal_checks_w() {
        assertTrue(Vec4f(1f, 2f, 3f, 4f).isFuzzyEqual(Vec4f(1f, 2f, 3f, 4f + 1e-6f)))
        assertFalse(Vec4f(1f, 2f, 3f, 4f).isFuzzyEqual(Vec4f(1f, 2f, 3f, 4.1f)))
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(Vec4f(1f, 2f, 3f, 4f), MutableVec4f(1f, 2f, 3f, 4f))
        assertEquals(Vec4f(1f, 2f, 3f, 4f).hashCode(), MutableVec4f(1f, 2f, 3f, 4f).hashCode())
        assertNotEquals(Vec4f(1f, 2f, 3f, 4f), Vec4f(1f, 2f, 3f, 5f))
    }

    @Test
    fun test_result_aliasing_this() {
        val v = MutableVec4f(1f, 2f, 3f, 4f)
        v.add(Vec4f(1f), v)
        assertEquals(Vec4f(2f, 3f, 4f, 5f), v)
        v.mul(Vec4f(2f), v)
        assertEquals(Vec4f(4f, 6f, 8f, 10f), v)
        v.subtract(Vec4f(4f, 6f, 8f, 10f), v)
        assertEquals(Vec4f.ZERO, v)
    }

    @Test
    fun test_result_aliasing_other() {
        val other = MutableVec4f(4f, 3f, 2f, 1f)
        Vec4f(1f, 2f, 3f, 4f).subtract(other, other)
        assertEquals(Vec4f(-3f, -1f, 1f, 3f), other)
    }

    @Test
    fun test_mat4_transform() {
        val m = Mat4().setToTranslate(1f, 2f, 3f)
        assertEquals(Vec4f(1f, 2f, 3f, 1f), m.transform(MutableVec4f(0f, 0f, 0f, 1f)))
        assertEquals(Vec4f(1f, 0f, 0f, 0f), m.transform(MutableVec4f(1f, 0f, 0f, 0f)))
        assertEquals(Vec4f(2f, 2f, 3f, 1f), m.transform(Vec4f(1f, 0f, 0f, 1f), MutableVec4f()))
    }

    @Test
    fun test_vec4i() {
        val v = MutableVec4i(1, 2, 3, 4)
        v.add(Vec4i(1))
        assertEquals(Vec4i(2, 3, 4, 5), v)
        v.subtract(Vec4i(2, 3, 4, 5))
        assertEquals(Vec4i.ZERO, v)
        v.set(1, 2, 3, 4)
        v[3] = 9
        assertEquals(9, v.w)
        assertEquals(Vec4i(1, 2, 3, 9), v)
        assertEquals(Vec4i(1, 2, 3, 4).hashCode(), MutableVec4i(1, 2, 3, 4).hashCode())
        assertNotEquals(Vec4i(1, 2, 3, 4), Vec4i(1, 2, 3, 5))
        assertEquals(Vec4i(5, 5, 5, 5), MutableVec4i().set(Vec4i(5)))
    }
}
