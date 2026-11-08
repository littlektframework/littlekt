package com.littlekt.math

import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class QuaternionTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun assertNear(expected: Quaternion, actual: Quaternion, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun assertSameRotation(expected: Quaternion, actual: Quaternion, eps: Float = 1e-5f) {
        assertTrue(
            abs(abs(expected.dot(actual)) - 1f) <= eps,
            "expected $expected but was $actual",
        )
    }

    private fun axisAngle(axis: Vec3f, angle: Angle): MutableQuaternion =
        MutableQuaternion().rotate(angle, axis)

    private fun MutableVec3f.rotatedBy(q: Quaternion): MutableVec3f = q.transform(this, this)

    @Test
    fun test_times_scalar() {
        val result = MutableQuaternion(1f, 2f, 3f, 4f) * 2f
        assertEquals(2f, result.x)
        assertEquals(4f, result.y)
        assertEquals(6f, result.z)
        assertEquals(8f, result.w)
    }

    @Test
    fun test_identity() {
        assertNear(Quaternion.IDENTITY, Quaternion())
        assertNear(Quaternion.IDENTITY, MutableQuaternion())
        assertNear(Quaternion.IDENTITY, MutableQuaternion(1f, 2f, 3f, 4f).identity())
        val v = Vec3f(1f, -2f, 3f)
        assertNear(v, Quaternion.IDENTITY.transform(v, MutableVec3f()))
    }

    @Test
    fun test_axis_angle_components() {
        val q = axisAngle(Vec3f.Z_AXIS, 90.degrees)
        val h = sqrt(0.5f)
        assertNear(Quaternion(0f, 0f, h, h), q)
        assertNear(1f, q.length())
    }

    @Test
    fun test_rotate_normalizes_axis() {
        assertNear(axisAngle(Vec3f.Z_AXIS, 40.degrees), axisAngle(Vec3f(0f, 0f, 5f), 40.degrees))
    }

    @Test
    fun test_rotate_composes_about_same_axis() {
        val q = axisAngle(Vec3f(1f, 2f, 3f), 20.degrees).rotate(50.degrees, Vec3f(1f, 2f, 3f))
        assertNear(axisAngle(Vec3f(1f, 2f, 3f), 70.degrees), q)
    }

    @Test
    fun test_transform_rotates_90_about_each_axis() {
        val z = axisAngle(Vec3f.Z_AXIS, 90.degrees)
        assertNear(Vec3f.Y_AXIS, MutableVec3f(Vec3f.X_AXIS).rotatedBy(z))
        val x = axisAngle(Vec3f.X_AXIS, 90.degrees)
        assertNear(Vec3f.Z_AXIS, MutableVec3f(Vec3f.Y_AXIS).rotatedBy(x))
        val y = axisAngle(Vec3f.Y_AXIS, 90.degrees)
        assertNear(Vec3f.X_AXIS, MutableVec3f(Vec3f.Z_AXIS).rotatedBy(y))
    }

    @Test
    fun test_transform_agrees_with_vec3_rotate() {
        val axis = Vec3f(1f, 2f, 3f).norm(MutableVec3f())
        val v = Vec3f(0.5f, -1f, 2f)
        val expected = v.rotate(65.degrees, axis, MutableVec3f())
        assertNear(expected, axisAngle(axis, 65.degrees).transform(v, MutableVec3f()))
    }

    @Test
    fun test_transform_agrees_with_mat4_rotation() {
        val q = axisAngle(Vec3f(-1f, 0.5f, 2f), 110.degrees)
        val m = Mat4().setToRotation(q)
        val v = Vec3f(3f, 1f, -2f)
        assertNear(m.transform(v, 0f, MutableVec3f()), q.transform(v, MutableVec3f()), 1e-4f)
        assertNear(Mat4().setToRotation(Vec3f(-1f, 0.5f, 2f), 110.degrees), m)
    }

    private fun assertNear(expected: Mat4, actual: Mat4, eps: Float = 1e-5f) {
        for (i in 0..15) {
            assertTrue(
                abs(expected[i] - actual[i]) <= eps,
                "index $i: ${expected[i]} vs ${actual[i]}",
            )
        }
    }

    @Test
    fun test_transform_preserves_length() {
        val q = axisAngle(Vec3f(3f, -1f, 2f), 77.degrees)
        assertNear(5f, q.transform(Vec3f(3f, 4f, 0f), MutableVec3f()).length(), 1e-4f)
    }

    @Test
    fun test_mul_applies_right_first() {
        val a = axisAngle(Vec3f.Z_AXIS, 90.degrees)
        val b = axisAngle(Vec3f.X_AXIS, 90.degrees)
        val v = Vec3f(0f, 1f, 0f)
        val expected = a.transform(b.transform(v, MutableVec3f()), MutableVec3f())
        assertNear(expected, (a * b).transform(v, MutableVec3f()))
        assertNear(Vec3f.Z_AXIS, expected)
    }

    @Test
    fun test_mul_identity() {
        val q = axisAngle(Vec3f(1f, 1f, 0f), 30.degrees)
        assertNear(q, q * Quaternion.IDENTITY)
        assertNear(q, Quaternion.IDENTITY * q)
    }

    @Test
    fun test_mul_matches_mat4_product() {
        val a = axisAngle(Vec3f(1f, 2f, 0f), 40.degrees)
        val b = axisAngle(Vec3f(0f, -1f, 3f), 25.degrees)
        val expected = Mat4().setToRotation(a).mul(Mat4().setToRotation(b))
        assertNear(expected, Mat4().setToRotation(a * b))
    }

    @Test
    fun test_mul_left_is_reverse_order() {
        val a = axisAngle(Vec3f(1f, 2f, 0f), 40.degrees)
        val b = axisAngle(Vec3f(0f, -1f, 3f), 25.degrees)
        assertNear(b * a, a.mulLeft(b, MutableQuaternion()))
        assertNear(b * a, MutableQuaternion(a).mulLeft(b))
        assertNear(a * b, MutableQuaternion(a).mul(b))
    }

    @Test
    fun test_mul_result_aliasing_this() {
        val a = axisAngle(Vec3f(1f, 2f, 0f), 40.degrees)
        val b = axisAngle(Vec3f(0f, -1f, 3f), 25.degrees)
        val m = MutableQuaternion(a)
        m.mul(b, m)
        assertNear(a * b, m)
    }

    @Test
    fun test_mul_result_aliasing_other() {
        val a = axisAngle(Vec3f(1f, 2f, 0f), 40.degrees)
        val b = axisAngle(Vec3f(0f, -1f, 3f), 25.degrees)
        val m = MutableQuaternion(b)
        a.mul(m, m)
        assertNear(a * b, m)
    }

    @Test
    fun test_mul_left_result_aliasing_other() {
        val a = axisAngle(Vec3f(1f, 2f, 0f), 40.degrees)
        val b = axisAngle(Vec3f(0f, -1f, 3f), 25.degrees)
        val m = MutableQuaternion(b)
        a.mulLeft(m, m)
        assertNear(b * a, m)
    }

    @Test
    fun test_mul_self() {
        val a = axisAngle(Vec3f.Z_AXIS, 30.degrees)
        assertNear(axisAngle(Vec3f.Z_AXIS, 60.degrees), MutableQuaternion(a).let { it.mul(it) })
    }

    @Test
    fun test_conjugate() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        assertNear(Quaternion(-1f, -2f, -3f, 4f), q.conjugate())
        val unit = axisAngle(Vec3f(1f, 1f, 1f), 50.degrees)
        assertNear(Quaternion.IDENTITY, unit * unit.conjugate(MutableQuaternion()))
    }

    @Test
    fun test_conjugate_undoes_rotation() {
        val q = axisAngle(Vec3f(1f, -1f, 2f), 80.degrees)
        val v = Vec3f(1f, 2f, 3f)
        val rotated = q.transform(v, MutableVec3f())
        val back = q.conjugate(MutableQuaternion()).transform(rotated, MutableVec3f())
        assertNear(v, back, 1e-4f)
    }

    @Test
    fun test_invert() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        assertNear(Quaternion.IDENTITY, q * q.invert())
        assertNear(Quaternion.IDENTITY, q.invert() * q)
        val unit = axisAngle(Vec3f(0f, 1f, 1f), 60.degrees)
        assertNear(unit.conjugate(MutableQuaternion()), unit.invert(MutableQuaternion()))
    }

    @Test
    fun test_invert_in_place() {
        val q = MutableQuaternion(1f, 2f, 3f, 4f)
        assertSame(q, q.invert())
        assertNear(Quaternion.IDENTITY, q * Quaternion(1f, 2f, 3f, 4f))
        val r = MutableQuaternion(1f, 2f, 3f, 4f)
        r.invert(r)
        assertNear(q, r)
    }

    @Test
    fun test_norm() {
        val n = Quaternion(1f, 2f, 3f, 4f).norm()
        assertNear(1f, n.length())
        val s = 1f / sqrt(30f)
        assertNear(Quaternion(s, 2f * s, 3f * s, 4f * s), n)
        val m = MutableQuaternion(0f, 0f, 2f, 0f)
        m.norm(m)
        assertNear(Quaternion(0f, 0f, 1f, 0f), m)
    }

    @Test
    fun test_length_dot_and_distance() {
        val a = Quaternion(1f, 2f, 3f, 4f)
        assertEquals(30f, a.sqrLength())
        assertNear(sqrt(30f), a.length())
        assertEquals(20f, a.dot(Quaternion(4f, 3f, 2f, 1f)))
        assertEquals(4f, Quaternion(0f, 0f, 5f, 1f).sqrDistance(Quaternion(0f, 0f, 5f, 3f)))
        assertEquals(2f, Quaternion(0f, 0f, 5f, 1f).distance(Quaternion(0f, 0f, 5f, 3f)))
        assertEquals(30f, a.sqrDistance(Quaternion(0f, 0f, 0f, 0f)))
    }

    @Test
    fun test_plus_minus() {
        val a = Quaternion(1f, 2f, 3f, 4f)
        val b = Quaternion(4f, 3f, 2f, 1f)
        assertNear(Quaternion(5f, 5f, 5f, 5f), a + b)
        assertNear(Quaternion(-3f, -1f, 1f, 3f), a - b)
        assertNear(Quaternion(5f, 5f, 5f, 5f), MutableQuaternion(a).add(Vec4f(4f, 3f, 2f, 1f)))
        assertNear(
            Quaternion(-3f, -1f, 1f, 3f),
            MutableQuaternion(a).subtract(Vec4f(4f, 3f, 2f, 1f)),
        )
    }

    @Test
    fun test_add_subtract_result_aliasing_other() {
        val a = Quaternion(1f, 2f, 3f, 4f)
        val m = MutableQuaternion(4f, 3f, 2f, 1f)
        a.subtract(m, m)
        assertNear(Quaternion(-3f, -1f, 1f, 3f), m)
        val n = MutableQuaternion(4f, 3f, 2f, 1f)
        a.add(n, n)
        assertNear(Quaternion(5f, 5f, 5f, 5f), n)
    }

    @Test
    fun test_scale() {
        assertNear(Quaternion(0.5f, 1f, 1.5f, 2f), Quaternion(1f, 2f, 3f, 4f).scale(0.5f))
    }

    @Test
    fun test_components_and_index_set() {
        val (x, y, z, w) = Quaternion(1f, 2f, 3f, 4f)
        assertEquals(listOf(1f, 2f, 3f, 4f), listOf(x, y, z, w))
        val q = MutableQuaternion()
        q[0] = 5f
        q[1] = 6f
        q[2] = 7f
        q[3] = 8f
        assertNear(Quaternion(5f, 6f, 7f, 8f), q)
        assertNear(Quaternion(1f, 2f, 3f, 4f), q.set(Vec4f(1f, 2f, 3f, 4f)))
    }

    @Test
    fun test_is_fuzzy_equal_checks_all_components() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        assertTrue(q.isFuzzyEqual(Quaternion(1f, 2f, 3f + 1e-6f, 4f)))
        assertFalse(q.isFuzzyEqual(Quaternion(1f, 2f, 3.5f, 4f)))
        assertFalse(q.isFuzzyEqual(Quaternion(1f, 2f, 3f, 4.5f)))
        assertTrue(q.isFuzzyEqual(Quaternion(1f, 2f, 3f, 4.5f), 0.6f))
    }

    @Test
    fun test_equals_same_type() {
        assertEquals(Quaternion(1f, 2f, 3f, 4f), Quaternion(1f, 2f, 3f, 4f))
        assertEquals(MutableQuaternion(1f, 2f, 3f, 4f), MutableQuaternion(1f, 2f, 3f, 4f))
        assertEquals(
            Quaternion(1f, 2f, 3f, 4f).hashCode(),
            MutableQuaternion(1f, 2f, 3f, 4f).hashCode(),
        )
    }

    @Test
    fun test_mix_endpoints() {
        val a = axisAngle(Vec3f.Z_AXIS, 0.degrees)
        val b = axisAngle(Vec3f.Z_AXIS, 90.degrees)
        assertNear(a, a.mix(b, 0f, MutableQuaternion()))
        assertNear(b, a.mix(b, 1f, MutableQuaternion()))
    }

    @Test
    fun test_mix_midpoint_normalized_is_half_rotation() {
        val a = Quaternion.IDENTITY
        val b = axisAngle(Vec3f.Z_AXIS, 90.degrees)
        val mid = a.mix(b, 0.5f, MutableQuaternion()).norm()
        assertNear(axisAngle(Vec3f.Z_AXIS, 45.degrees), mid)
    }

    @Test
    fun test_mix_result_aliasing_other() {
        val a = Quaternion(0f, 0f, 0f, 2f)
        val m = MutableQuaternion(2f, 0f, 0f, 0f)
        a.mix(m, 0.5f, m)
        assertNear(Quaternion(1f, 0f, 0f, 1f), m)
    }

    @Test
    fun test_set_euler_single_axes() {
        assertNear(
            axisAngle(Vec3f.X_AXIS, 30.degrees),
            Quaternion.IDENTITY.setEuler(30.degrees, 0.degrees, 0.degrees),
        )
        assertNear(
            axisAngle(Vec3f.Y_AXIS, 30.degrees),
            Quaternion.IDENTITY.setEuler(0.degrees, 30.degrees, 0.degrees),
        )
        assertNear(
            axisAngle(Vec3f.Z_AXIS, 30.degrees),
            Quaternion.IDENTITY.setEuler(0.degrees, 0.degrees, 30.degrees),
        )
    }

    @Test
    fun test_set_euler_order_is_yaw_pitch_roll() {
        val expected =
            axisAngle(Vec3f.Y_AXIS, 40.degrees) *
                axisAngle(Vec3f.X_AXIS, 25.degrees) *
                axisAngle(Vec3f.Z_AXIS, -60.degrees)
        assertNear(expected, MutableQuaternion().setEuler(25.degrees, 40.degrees, (-60).degrees))
    }

    @Test
    fun test_euler_round_trip() {
        val q = MutableQuaternion().setEuler(20.degrees, 35.degrees, (-50).degrees)
        assertNear(20f, q.pitch.degrees, 1e-3f)
        assertNear(35f, q.yaw.degrees, 1e-3f)
        assertNear(-50f, q.roll.degrees, 1e-3f)
        assertEquals(0, q.gimbalPole)
    }

    @Test
    fun test_euler_round_trip_roll_90() {
        val q = MutableQuaternion().setEuler(0.degrees, 0.degrees, 90.degrees)
        assertNear(0f, q.pitch.degrees, 1e-3f)
        assertNear(0f, q.yaw.degrees, 1e-3f)
        assertNear(90f, q.roll.degrees, 1e-3f)
        assertEquals(0, q.gimbalPole)
    }

    @Test
    fun test_euler_round_trip_yaw_120() {
        val q = MutableQuaternion().setEuler(10.degrees, 120.degrees, 5.degrees)
        assertNear(10f, q.pitch.degrees, 1e-3f)
        assertNear(120f, q.yaw.degrees, 1e-3f)
        assertNear(5f, q.roll.degrees, 1e-3f)
    }

    @Test
    fun test_euler_gimbal_poles_reproduce_rotation() {
        for (pitch in listOf(90f, -90f)) {
            val q = MutableQuaternion().setEuler(pitch.degrees, 30.degrees, 20.degrees)
            assertEquals(if (pitch > 0f) 1 else -1, q.gimbalPole)
            assertNear(pitch, q.pitch.degrees, 1e-3f)
            val rebuilt = MutableQuaternion().setEuler(q.pitch, q.yaw, q.roll)
            assertSameRotation(q, rebuilt, 1e-4f)
        }
    }

    @Test
    fun test_look_at_rotates_z_onto_forward() {
        val q = MutableQuaternion().lookAt(Vec3f.X_AXIS, Vec3f.UP)
        assertNear(Vec3f.X_AXIS, MutableVec3f(Vec3f.FORWARD).rotatedBy(q))
        assertNear(Vec3f.UP, MutableVec3f(Vec3f.UP).rotatedBy(q))
    }

    @Test
    fun test_look_at_arbitrary_direction() {
        val forward = Vec3f(1f, 1f, -1f).norm(MutableVec3f())
        val q = Quaternion.IDENTITY.lookAt(forward, Vec3f.UP, MutableQuaternion())
        assertNear(forward, MutableVec3f(Vec3f.FORWARD).rotatedBy(q), 1e-4f)
        assertNear(1f, q.length())
        val back = MutableQuaternion().lookAt(Vec3f.NEG_Z_AXIS, Vec3f.UP)
        assertNear(Vec3f.NEG_Z_AXIS, MutableVec3f(Vec3f.FORWARD).rotatedBy(back), 1e-4f)
    }

    @Test
    fun test_get_rotation_from_mat4_round_trip() {
        val q = axisAngle(Vec3f(2f, -1f, 0.5f), 140.degrees)
        val back = Mat4().setToRotation(q).getRotation(MutableQuaternion())
        assertSameRotation(q, back, 1e-4f)
    }
}
