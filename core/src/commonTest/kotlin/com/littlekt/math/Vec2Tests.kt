package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Vec2Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec2f, actual: Vec2f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    @Test
    fun test_constructors() {
        assertEquals(Vec2f(2f, 2f), Vec2f(2f))
        assertEquals(Vec2f(1f, 2f), Vec2f(Vec2f(1f, 2f)))
        assertEquals(Vec2f(0f, 0f), MutableVec2f())
        assertEquals(Vec2f(3f, 3f), MutableVec2f(3f))
    }

    @Test
    fun test_add() {
        assertEquals(Vec2f(4f, 6f), Vec2f(1f, 2f).add(Vec2f(3f, 4f), MutableVec2f()))
        assertEquals(Vec2f(4f, 6f), MutableVec2f(1f, 2f).add(3f, 4f))
    }

    @Test
    fun test_subtract() {
        assertEquals(Vec2f(-2f, -2f), Vec2f(1f, 2f).subtract(Vec2f(3f, 4f), MutableVec2f()))
        assertEquals(Vec2f(-2f, -2f), MutableVec2f(1f, 2f).subtract(3f, 4f))
    }

    @Test
    fun test_scale_and_mul() {
        assertEquals(Vec2f(2f, 4f), Vec2f(1f, 2f).scale(2f, MutableVec2f()))
        assertEquals(Vec2f(3f, 8f), Vec2f(1f, 2f).mul(Vec2f(3f, 4f), MutableVec2f()))
        assertEquals(Vec2f(2f, 6f), MutableVec2f(1f, 2f).scale(2f, 3f))
    }

    @Test
    fun test_operators() {
        val v = MutableVec2f(2f, 4f)
        v += Vec2f(1f, 1f)
        assertEquals(Vec2f(3f, 5f), v)
        v -= Vec2f(1f, 1f)
        assertEquals(Vec2f(2f, 4f), v)
        v *= 2f
        assertEquals(Vec2f(4f, 8f), v)
        v /= 4f
        assertEquals(Vec2f(1f, 2f), v)
        v /= Vec2f(1f, 2f)
        assertEquals(Vec2f(1f, 1f), v)
        assertEquals(11f, Vec2f(1f, 2f) * Vec2f(3f, 4f))
    }

    @Test
    fun test_mul_add() {
        assertEquals(Vec2f(7f, 10f), MutableVec2f(1f, 2f).mulAdd(Vec2f(3f, 4f), 2f))
        assertEquals(Vec2f(4f, 10f), MutableVec2f(1f, 2f).mulAdd(Vec2f(3f, 4f), Vec2f(1f, 2f)))
    }

    @Test
    fun test_length_and_distance() {
        assertEquals(5f, Vec2f(3f, 4f).length())
        assertEquals(25f, Vec2f(3f, 4f).sqrLength())
        assertEquals(5f, Vec2f(1f, 1f).distance(Vec2f(4f, 5f)))
        assertEquals(25f, Vec2f(1f, 1f).sqrDistance(Vec2f(4f, 5f)))
    }

    @Test
    fun test_dot() {
        assertEquals(11f, Vec2f(1f, 2f).dot(Vec2f(3f, 4f)))
        assertEquals(0f, Vec2f.X_AXIS.dot(Vec2f.Y_AXIS))
    }

    @Test
    fun test_norm() {
        val n = Vec2f(3f, 4f).norm(MutableVec2f())
        assertNear(1f, n.length())
        assertNear(Vec2f(0.6f, 0.8f), n)
    }

    @Test
    fun test_norm_zero_stays_zero() {
        assertEquals(Vec2f.ZERO, MutableVec2f().norm())
    }

    @Test
    fun test_set_length() {
        val v = Vec2f(3f, 4f).setLength(10f, MutableVec2f())
        assertNear(Vec2f(6f, 8f), v)
        assertEquals(Vec2f.ZERO, MutableVec2f().setLength(5f))
    }

    @Test
    fun test_mix_and_lerp() {
        val a = Vec2f(0f, 10f)
        val b = Vec2f(10f, 20f)
        assertEquals(a, a.mix(b, 0f, MutableVec2f()))
        assertEquals(b, a.mix(b, 1f, MutableVec2f()))
        assertEquals(Vec2f(5f, 15f), a.mix(b, 0.5f, MutableVec2f()))
        assertEquals(Vec2f(2.5f, 12.5f), MutableVec2f(a).lerp(b, 0.25f))
    }

    @Test
    fun test_rotate_90_maps_x_to_y() {
        assertNear(Vec2f.Y_AXIS, MutableVec2f(1f, 0f).rotate(90.degrees))
        assertNear(Vec2f.NEG_X_AXIS, Vec2f.Y_AXIS.rotate(90.degrees, MutableVec2f()))
    }

    @Test
    fun test_rotate_preserves_length() {
        val v = MutableVec2f(3f, 4f).rotate(33.degrees)
        assertNear(5f, v.length())
    }

    @Test
    fun test_angle_to() {
        assertNear(90f, Vec2f.X_AXIS.angleTo(Vec2f.Y_AXIS).degrees, 1e-4f)
        assertNear(-90f, Vec2f.Y_AXIS.angleTo(Vec2f.X_AXIS).degrees, 1e-4f)
        assertNear(0f, Vec2f(2f, 2f).angleTo(Vec2f(1f, 1f)).degrees, 1e-4f)
    }

    @Test
    fun test_mul_mat3_applies_translation() {
        val m = Mat3().setToTranslate(5f, 6f).rotate(Vec3f.Z_AXIS, 90f)
        assertNear(Vec2f(5f, 7f), MutableVec2f(1f, 0f).mul(m))
    }

    @Test
    fun test_mul_mat4_applies_translation() {
        val m = Mat4().setToTranslate(5f, 6f, 7f).rotate(Vec3f.Z_AXIS, 90.degrees)
        assertNear(Vec2f(5f, 7f), MutableVec2f(1f, 0f).mul(m))
    }

    @Test
    fun test_is_fuzzy_equal() {
        assertTrue(Vec2f(1f, 2f).isFuzzyEqual(Vec2f(1f + 1e-6f, 2f - 1e-6f)))
        assertFalse(Vec2f(1f, 2f).isFuzzyEqual(Vec2f(1.1f, 2f)))
        assertTrue(Vec2f(1f, 2f).isFuzzyEqual(Vec2f(1.1f, 2f), 0.2f))
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(Vec2f(1f, 2f), MutableVec2f(1f, 2f))
        assertEquals(Vec2f(1f, 2f).hashCode(), MutableVec2f(1f, 2f).hashCode())
        assertNotEquals(Vec2f(1f, 2f), Vec2f(2f, 1f))
    }

    @Test
    fun test_equals_is_symmetric_across_sizes() {
        assertNotEquals<Vec2f>(Vec2f(1f, 2f), Vec3f(1f, 2f, 3f))
        assertNotEquals<Vec2f>(Vec3f(1f, 2f, 3f), Vec2f(1f, 2f))
        assertNotEquals<Vec3f>(Vec3f(1f, 2f, 3f), Vec4f(1f, 2f, 3f, 4f))
    }

    @Test
    fun test_set_and_index() {
        val v = MutableVec2f()
        assertSame(v, v.set(1f, 2f))
        assertEquals(1f, v[0])
        assertEquals(2f, v[1])
        v[0] = 5f
        assertEquals(5f, v.x)
        assertEquals(Vec2f(3f, 4f), v.set(Vec2f(3f, 4f)))
    }

    @Test
    fun test_conversions() {
        val v = MutableVec2f(1f, 2f)
        val copy = v.toMutableVec2()
        copy.x = 9f
        assertEquals(1f, v.x)
        assertEquals(Vec2f(1f, 2f), v.toVec2())
    }

    @Test
    fun test_result_aliasing_this() {
        val v = MutableVec2f(3f, 4f)
        v.add(Vec2f(1f, 1f), v)
        assertEquals(Vec2f(4f, 5f), v)
        v.mix(Vec2f(0f, 0f), 0.5f, v)
        assertEquals(Vec2f(2f, 2.5f), v)
        v.norm(v)
        assertNear(1f, v.length())
        v.set(1f, 0f).rotate(90.degrees, v)
        assertNear(Vec2f.Y_AXIS, v)
    }

    @Test
    fun test_result_aliasing_other() {
        val other = MutableVec2f(3f, 4f)
        Vec2f(1f, 2f).subtract(other, other)
        assertEquals(Vec2f(-2f, -2f), other)
        val o2 = MutableVec2f(10f, 20f)
        Vec2f(0f, 0f).mix(o2, 0.5f, o2)
        assertEquals(Vec2f(5f, 10f), o2)
    }

    @Test
    fun test_diagonal_length() {
        assertNear(sqrt(2f), Vec2f(1f, 1f).length())
    }

    @Test
    fun test_vec2i() {
        val v = MutableVec2i(1, 2)
        v.add(Vec2i(3, 4))
        assertEquals(Vec2i(4, 6), v)
        v.subtract(Vec2i(1, 1))
        assertEquals(Vec2i(3, 5), v)
        v.setAll(7, 8)
        assertEquals(7, v[0])
        assertEquals(8, v.y)
        v[1] = 9
        assertEquals(Vec2i(7, 9), v)
        assertEquals(Vec2i(2, 2), Vec2i(2))
        assertEquals(Vec2i(1, 2).hashCode(), MutableVec2i(1, 2).hashCode())
        assertNotEquals(Vec2i(1, 2), Vec2i(2, 1))
        assertEquals(Vec2i(1, 2), MutableVec2i().set(Vec2i(1, 2)))
    }
}
