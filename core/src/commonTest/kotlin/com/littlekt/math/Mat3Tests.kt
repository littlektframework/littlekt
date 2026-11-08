package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Mat3Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec2f, actual: Vec2f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun assertNear(expected: Mat3, actual: Mat3, eps: Float = 1e-5f) {
        for (i in 0..8) {
            assertTrue(
                abs(expected[i] - actual[i]) <= eps,
                "index $i expected ${expected[i]} but was ${actual[i]}\n$actual",
            )
        }
    }

    private fun mat(vararg values: Float): Mat3 = Mat3().apply { set(values.toList()) }

    private val a = mat(2f, 1f, 0f, 0f, 3f, 1f, 1f, 0f, 4f)
    private val b = mat(1f, 2f, 3f, 0f, 1f, 4f, 5f, 6f, 0f)

    @Test
    fun test_identity() {
        val m = Mat3()
        assertEquals(listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), m.toList())
        assertEquals(Vec3f(1f, 2f, 3f), m.transform(MutableVec3f(1f, 2f, 3f)))
    }

    @Test
    fun test_column_major_accessors() {
        val m = mat(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        assertEquals(3f, m.m01)
        assertEquals(1f, m.m10)
        assertEquals(6f, m.m02)
        assertEquals(7f, m[1, 2])
        m[2, 0] = 9f
        assertEquals(9f, m.data[2])
    }

    @Test
    fun test_mul_identity() {
        assertNear(a, Mat3().mul(a, Mat3()))
        assertNear(a, a.mul(Mat3(), Mat3()))
    }

    @Test
    fun test_mul_matches_hand_computed() {
        val expected = mat(5f, 7f, 14f, 4f, 3f, 17f, 10f, 23f, 6f)
        assertNear(expected, a.mul(b, Mat3()))
    }

    @Test
    fun test_mul_order_transforms_right_first() {
        val ab = a.mul(b, Mat3())
        val v = Vec3f(1f, -2f, 3f)
        val expected = a.transform(b.transform(v, MutableVec3f()), MutableVec3f())
        assertNear(expected, ab.transform(v, MutableVec3f()))
    }

    @Test
    fun test_mul_left_is_reverse_order() {
        assertNear(b.mul(a, Mat3()), a.mulLeft(b, Mat3()))
        assertNear(b.mul(a, Mat3()), Mat3().set(a).mulLeft(b))
    }

    @Test
    fun test_mul_in_place() {
        assertNear(a.mul(b, Mat3()), Mat3().set(a).mul(b))
    }

    @Test
    fun test_mul_result_aliasing_this() {
        val m = Mat3().set(a)
        m.mul(b, m)
        assertNear(a.mul(b, Mat3()), m)
    }

    @Test
    fun test_mul_result_aliasing_other() {
        val m = Mat3().set(b)
        a.mul(m, m)
        assertNear(a.mul(b, Mat3()), m)
    }

    @Test
    fun test_mul_left_result_aliasing_this() {
        val m = Mat3().set(a)
        m.mulLeft(b, m)
        assertNear(b.mul(a, Mat3()), m)
    }

    @Test
    fun test_transpose() {
        val t = a.transpose(Mat3())
        for (r in 0..2) for (c in 0..2) assertEquals(a[r, c], t[c, r])
        assertNear(t, Mat3().set(a).transpose())
    }

    @Test
    fun test_transpose_result_aliasing_this() {
        val m = Mat3().set(a)
        m.transpose(m)
        assertNear(a.transpose(Mat3()), m)
    }

    @Test
    fun test_invert_round_trip() {
        val inv = Mat3()
        assertTrue(a.invert(inv))
        assertNear(Mat3(), a.mul(inv, Mat3()))
        assertNear(Mat3(), inv.mul(a, Mat3()))
    }

    @Test
    fun test_invert_negative_determinant() {
        val m = Mat3().setToScale(-2f, 4f)
        val inv = Mat3()
        assertTrue(m.invert(inv))
        assertNear(Mat3().setToScale(-0.5f, 0.25f), inv)
    }

    @Test
    fun test_invert_in_place() {
        val m = Mat3().set(a)
        assertTrue(m.invert())
        assertNear(Mat3(), a.mul(m, Mat3()))
    }

    @Test
    fun test_invert_result_aliasing_this() {
        val m = Mat3().set(a)
        assertTrue(m.invert(m))
        assertNear(Mat3(), a.mul(m, Mat3()))
    }

    @Test
    fun test_invert_singular_returns_false_and_leaves_matrix() {
        val m = mat(1f, 2f, 3f, 2f, 4f, 6f, 0f, 0f, 1f)
        val copy = Mat3().set(m)
        assertFalse(m.invert())
        assertEquals(copy, m)
    }

    @Test
    fun test_set_to_rotation_angle_maps_x_to_y() {
        val m = Mat3().setToRotation(90.degrees)
        assertNear(Vec2f(0f, 1f), m.transform(Vec2f(1f, 0f), MutableVec2f()))
        assertNear(90f, m.rotation.degrees, 1e-4f)
    }

    @Test
    fun test_set_to_rotation_axis_degrees() {
        val m = Mat3().setToRotation(0f, 0f, 1f, 90f)
        assertNear(Vec3f.Y_AXIS, m.transform(MutableVec3f(Vec3f.X_AXIS)))
        assertNear(Mat3().setToRotation(90.degrees), m)
    }

    @Test
    fun test_set_to_rotation_normalizes_axis() {
        assertNear(Mat3().setToRotation(0f, 0f, 1f, 30f), Mat3().setToRotation(0f, 0f, 5f, 30f))
    }

    @Test
    fun test_set_to_rotation_agrees_with_mat4() {
        val axis = Vec3f(1f, -2f, 0.5f).norm(MutableVec3f())
        val m3 = Mat3().setToRotation(axis.x, axis.y, axis.z, 40f)
        val m4 = Mat4().setToRotation(axis, 40.degrees)
        val v = Vec3f(0.2f, 1f, -3f)
        assertNear(m4.transform(v, 0f, MutableVec3f()), m3.transform(v, MutableVec3f()))
        assertNear(m3, m4.getRotation(Mat3()))
    }

    @Test
    fun test_set_from_euler_angles_single_axis() {
        val z = Mat3().setFromEulerAngles(0f, 0f, 90f)
        assertNear(Vec3f.Y_AXIS, z.transform(MutableVec3f(Vec3f.X_AXIS)))
        val x = Mat3().setFromEulerAngles(90f, 0f, 0f)
        assertNear(Vec3f.Z_AXIS, x.transform(MutableVec3f(Vec3f.Y_AXIS)))
        val y = Mat3().setFromEulerAngles(0f, 90f, 0f)
        assertNear(Vec3f.X_AXIS, y.transform(MutableVec3f(Vec3f.Z_AXIS)))
    }

    @Test
    fun test_set_from_euler_angles_order_is_z_y_x() {
        val expected =
            Mat3()
                .setToRotation(0f, 0f, 1f, 30f)
                .mul(Mat3().setToRotation(0f, 1f, 0f, 45f))
                .mul(Mat3().setToRotation(1f, 0f, 0f, 60f))
        assertNear(expected, Mat3().setFromEulerAngles(60f, 45f, 30f))
    }

    @Test
    fun test_rotate_post_multiplies() {
        val m = Mat3().setToScale(2f, 3f).rotate(0f, 0f, 1f, 90f)
        val expected = Mat3().setToScale(2f, 3f).mul(Mat3().setToRotation(90.degrees))
        assertNear(expected, m)
        assertNear(expected, Mat3().setToScale(2f, 3f).rotate(Vec3f.Z_AXIS, 90f, Mat3()))
    }

    @Test
    fun test_translate_post_multiplies() {
        val m = Mat3().setToScale(2f, 3f).translate(1f, 1f)
        assertNear(Vec2f(2f, 3f), m.transform(Vec2f(0f, 0f), MutableVec2f()))
        assertNear(Vec2f(2f, 3f), m.getTranslation(MutableVec2f()))
        assertNear(m, Mat3().setToScale(2f, 3f).translate(1f, 1f, Mat3()))
    }

    @Test
    fun test_set_to_translate_transforms_point() {
        val m = Mat3().setToTranslate(5f, -2f)
        assertNear(Vec2f(6f, -1f), m.transform(MutableVec2f(1f, 1f)))
        assertEquals(Vec2f(5f, -2f), m.position)
    }

    @Test
    fun test_scale() {
        val m = Mat3().setToTranslate(1f, 2f).scale(2f, 3f)
        assertNear(Vec2f(3f, 5f), m.transform(MutableVec2f(1f, 1f)))
        assertEquals(Vec2f(2f, 3f), Mat3().setToScale(2f, 3f).scale)
    }

    @Test
    fun test_scale_into_result_keeps_translation() {
        val m = Mat3().setToTranslate(1f, 2f)
        val result = Mat3().setToZero()
        m.scale(2f, 3f, result)
        assertNear(Mat3().setToTranslate(1f, 2f).scale(2f, 3f), result)
    }

    @Test
    fun test_get_scale_of_uniform_rotated_matrix() {
        val m = Mat3().setToRotation(30.degrees).scale(2f, 2f)
        assertNear(Vec2f(2f, 2f), m.getScale(MutableVec2f()))
    }

    @Test
    fun test_col_vec() {
        val m = Mat3()
        m.setColVec(1, Vec3f(4f, 5f, 6f))
        assertEquals(Vec3f(4f, 5f, 6f), m.getColVec(1, MutableVec3f()))
        assertEquals(4f, m[3])
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(Mat3().set(a), Mat3().set(a))
        assertEquals(Mat3().set(a).hashCode(), Mat3().set(a).hashCode())
        assertFalse(Mat3() == Mat3().set(a))
    }

    @Test
    fun test_mat4_set_from_mat3_keeps_2d_affine() {
        val m3 = Mat3().setToTranslate(3f, 4f).rotate(0f, 0f, 1f, 90f)
        val m4 = Mat4().set(m3)
        assertNear(
            Vec3f(3f, 5f, 0f),
            m4.transform(MutableVec3f(1f, 0f, 0f)),
        )
    }

    private fun Mat3.setToZero(): Mat3 {
        for (i in 0..8) this[i] = 0f
        return this
    }
}
