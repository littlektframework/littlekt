package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Mat4Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-5f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun assertNear(expected: Mat4, actual: Mat4, eps: Float = 1e-5f) {
        for (i in 0..15) {
            assertTrue(
                abs(expected[i] - actual[i]) <= eps,
                "index $i expected ${expected[i]} but was ${actual[i]}",
            )
        }
    }

    private fun Mat4.clip(x: Float, y: Float, z: Float): MutableVec4f =
        transform(MutableVec4f(x, y, z, 1f))

    private fun trs(): Mat4 =
        Mat4().setToTranslate(1f, 2f, 3f).rotate(Vec3f(1f, 1f, 0f), 30.degrees).scale(2f, 3f, 4f)

    private fun other(): Mat4 =
        Mat4().setToTranslate(-4f, 0.5f, 2f).rotate(Vec3f(0f, 1f, 2f), 75.degrees).scale(0.5f)

    @Test
    fun test_orthographic_maps_corners_to_ndc() {
        val proj = Mat4().setToOrthographic(10f, 30f, 5f, 25f, 1f, 11f)
        val min = proj.clip(10f, 5f, -1f)
        val max = proj.clip(30f, 25f, -11f)

        assertNear(-1f, min.x)
        assertNear(-1f, min.y)
        assertNear(0f, min.z)
        assertNear(1f, max.x)
        assertNear(1f, max.y)
        assertNear(1f, max.z)
    }

    @Test
    fun test_orthographic_symmetric_keeps_origin_centered() {
        val proj = Mat4().setToOrthographic(-50f, 50f, -25f, 25f, 0f, 1f)
        val origin = proj.clip(0f, 0f, 0f)

        assertNear(0f, origin.x)
        assertNear(0f, origin.y)
        assertNear(0f, origin.z)
    }

    @Test
    fun test_orthographic_rejects_degenerate_planes() {
        assertFailsWith<IllegalArgumentException> {
            Mat4().setToOrthographic(1f, 1f, 0f, 1f, 0f, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            Mat4().setToOrthographic(0f, 1f, 2f, 2f, 0f, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            Mat4().setToOrthographic(0f, 1f, 0f, 1f, 3f, 3f)
        }
    }

    @Test
    fun test_perspective_maps_near_far_to_zero_one() {
        val proj = Mat4().setToPerspective(60.degrees, 1f, 0.1f, 100f)
        val near = proj.clip(0f, 0f, -0.1f)
        val far = proj.clip(0f, 0f, -100f)

        assertNear(0f, near.z / near.w)
        assertNear(1f, far.z / far.w, 1e-4f)
    }

    @Test
    fun test_perspective_maps_fov_edges_to_ndc_edges() {
        val aspect = 2f
        val proj = Mat4().setToPerspective(60.degrees, aspect, 0.1f, 100f)
        val d = 10f
        val halfHeight = d * tan(30.degrees.radians)
        val top = proj.clip(0f, halfHeight, -d)
        val right = proj.clip(halfHeight * aspect, 0f, -d)

        assertNear(1f, top.y / top.w)
        assertNear(1f, right.x / right.w)
        assertNear(d, top.w)
    }

    @Test
    fun test_identity() {
        val m = Mat4()
        for (r in 0..3) for (c in 0..3) assertEquals(if (r == c) 1f else 0f, m[r, c])
        assertEquals(Vec3f(1f, 2f, 3f), m.transform(MutableVec3f(1f, 2f, 3f)))
        m.set(trs())
        assertSame(m, m.setToIdentity())
        assertNear(Mat4(), m)
    }

    @Test
    fun test_column_major_layout() {
        val m = Mat4().setToTranslate(1f, 2f, 3f)
        assertEquals(1f, m.data[12])
        assertEquals(2f, m.data[13])
        assertEquals(3f, m.data[14])
        assertEquals(1f, m.m03)
        assertEquals(2f, m[1, 3])
        m[2, 1] = 7f
        assertEquals(7f, m.data[6])
        assertEquals(7f, m.m21)
    }

    @Test
    fun test_mul_order_translate_then_scale() {
        val t = Mat4().setToTranslate(1f, 2f, 3f)
        val s = Mat4().setToScaling(2f, 2f, 2f)
        assertEquals(Vec3f(1f, 2f, 3f), t.mul(s, Mat4()).getTranslation(MutableVec3f()))
        assertEquals(Vec3f(2f, 4f, 6f), s.mul(t, Mat4()).getTranslation(MutableVec3f()))
    }

    @Test
    fun test_mul_identity() {
        val a = trs()
        assertNear(a, a.mul(Mat4(), Mat4()))
        assertNear(a, Mat4().mul(a, Mat4()))
    }

    @Test
    fun test_mul_transforms_right_first() {
        val a = trs()
        val b = other()
        val v = Vec3f(0.3f, -1f, 2f)
        val expected = a.transform(b.transform(v, 1f, MutableVec3f()), 1f, MutableVec3f())
        assertNear(expected, a.mul(b, Mat4()).transform(v, 1f, MutableVec3f()), 1e-4f)
    }

    @Test
    fun test_mul_is_associative() {
        val a = trs()
        val b = other()
        val c = Mat4().setToRotation(Vec3f.Z_AXIS, 20.degrees).translate(1f, 0f, 0f)
        val left = a.mul(b, Mat4()).mul(c, Mat4())
        val right = a.mul(b.mul(c, Mat4()), Mat4())
        assertNear(left, right, 1e-4f)
    }

    @Test
    fun test_mul_in_place() {
        val a = trs()
        val b = other()
        assertNear(a.mul(b, Mat4()), Mat4().set(a).mul(b))
    }

    @Test
    fun test_mul_result_aliasing_this() {
        val a = trs()
        val b = other()
        val m = Mat4().set(a)
        m.mul(b, m)
        assertNear(a.mul(b, Mat4()), m)
    }

    @Test
    fun test_mul_result_aliasing_other() {
        val a = trs()
        val b = other()
        val m = Mat4().set(b)
        a.mul(m, m)
        assertNear(a.mul(b, Mat4()), m)
    }

    @Test
    fun test_mul_self() {
        val a = trs()
        val m = Mat4().set(a)
        m.mul(m)
        assertNear(a.mul(a, Mat4()), m, 1e-4f)
    }

    @Test
    fun test_mul_left_is_reverse_order() {
        val a = trs()
        val b = other()
        assertNear(b.mul(a, Mat4()), a.mulLeft(b, Mat4()))
        assertNear(b.mul(a, Mat4()), Mat4().set(a).mulLeft(b))
    }

    @Test
    fun test_mul_left_result_aliasing_this() {
        val a = trs()
        val b = other()
        val m = Mat4().set(a)
        m.mulLeft(b, m)
        assertNear(b.mul(a, Mat4()), m)
    }

    @Test
    fun test_invert_round_trip() {
        val a = trs()
        val inv = Mat4()
        a.invert(inv)
        assertNear(Mat4(), a.mul(inv, Mat4()))
        assertNear(Mat4(), inv.mul(a, Mat4()))
    }

    @Test
    fun test_invert_projection_round_trip() {
        val p = Mat4().setToPerspective(70.degrees, 1.5f, 0.5f, 50f)
        val inv = Mat4()
        p.invert(inv)
        assertNear(Mat4(), p.mul(inv, Mat4()), 1e-4f)
    }

    @Test
    fun test_invert_in_place() {
        val a = trs()
        val m = Mat4().set(a)
        assertSame(m, m.invert())
        assertNear(Mat4(), a.mul(m, Mat4()))
    }

    @Test
    fun test_invert_result_aliasing_this() {
        val a = trs()
        val m = Mat4().set(a)
        m.invert(m)
        assertNear(Mat4(), a.mul(m, Mat4()))
    }

    @Test
    fun test_invert_translation() {
        val inv = Mat4().setToTranslate(1f, 2f, 3f).invert()
        assertNear(Mat4().setToTranslate(-1f, -2f, -3f), inv)
    }

    @Test
    fun test_invert_singular_throws() {
        assertFailsWith<RuntimeException> { Mat4().setToScaling(1f, 0f, 1f).invert() }
    }

    @Test
    fun test_invert_inverts_point_transform() {
        val a = trs()
        val p = Vec3f(4f, -2f, 7f)
        val moved = a.transform(p, 1f, MutableVec3f())
        val inv = Mat4().set(a).invert()
        assertNear(p, inv.transform(moved), 1e-4f)
    }

    @Test
    fun test_transpose() {
        val a = trs()
        val t = a.transpose(Mat4())
        for (r in 0..3) for (c in 0..3) assertEquals(a[r, c], t[c, r])
        assertNear(t, Mat4().set(a).transpose())
    }

    @Test
    fun test_transpose_result_aliasing_this() {
        val a = trs()
        val m = Mat4().set(a)
        m.transpose(m)
        assertNear(a.transpose(Mat4()), m)
    }

    @Test
    fun test_transpose_of_rotation_is_inverse() {
        val r = Mat4().setToRotation(Vec3f(1f, 2f, 3f), 40.degrees)
        val inv = Mat4().set(r).invert()
        assertNear(inv, r.transpose(Mat4()))
    }

    @Test
    fun test_translate_post_multiplies() {
        val m = Mat4().setToScaling(2f, 3f, 4f).translate(1f, 1f, 1f)
        assertEquals(Vec3f(2f, 3f, 4f), m.getTranslation(MutableVec3f()))
        assertNear(m, Mat4().setToScaling(2f, 3f, 4f).translate(1f, 1f, 1f, Mat4()))
        assertNear(m, Mat4().setToScaling(2f, 3f, 4f).translate(Vec3f(1f)))
    }

    @Test
    fun test_translate_into_result_aliasing_this() {
        val m = Mat4().setToScaling(2f, 3f, 4f)
        m.translate(1f, 1f, 1f, m)
        assertEquals(Vec3f(2f, 3f, 4f), m.getTranslation(MutableVec3f()))
    }

    @Test
    fun test_rotation_90_about_each_axis() {
        assertNear(
            Vec3f.Y_AXIS,
            Mat4().setToRotation(Vec3f.Z_AXIS, 90.degrees).transform(MutableVec3f(Vec3f.X_AXIS)),
        )
        assertNear(
            Vec3f.Z_AXIS,
            Mat4().setToRotation(Vec3f.X_AXIS, 90.degrees).transform(MutableVec3f(Vec3f.Y_AXIS)),
        )
        assertNear(
            Vec3f.X_AXIS,
            Mat4().setToRotation(Vec3f.Y_AXIS, 90.degrees).transform(MutableVec3f(Vec3f.Z_AXIS)),
        )
    }

    @Test
    fun test_rotation_axis_shortcuts_match_general_case() {
        val angle = 35.degrees
        for (axis in listOf(Vec3f.X_AXIS, Vec3f.Y_AXIS, Vec3f.Z_AXIS)) {
            val expected = Mat4().setToRotation(MutableQuaternion().rotate(angle, axis))
            assertNear(expected, Mat4().setToRotation(axis, angle))
        }
    }

    @Test
    fun test_rotation_normalizes_axis() {
        assertNear(
            Mat4().setToRotation(Vec3f(1f, 1f, 0f).norm(MutableVec3f()), 30.degrees),
            Mat4().setToRotation(Vec3f(3f, 3f, 0f), 30.degrees),
        )
    }

    @Test
    fun test_rotate_post_multiplies() {
        val base = Mat4().setToTranslate(1f, 2f, 3f)
        val r = Mat4().setToRotation(Vec3f.Z_AXIS, 90.degrees)
        val expected = base.mul(r, Mat4())
        assertNear(expected, Mat4().set(base).rotate(Vec3f.Z_AXIS, 90.degrees))
        assertNear(expected, base.rotate(Vec3f.Z_AXIS, 90.degrees, Mat4()))
    }

    @Test
    fun test_rotate_into_result_aliasing_this() {
        val base = Mat4().setToTranslate(1f, 2f, 3f)
        val expected = base.mul(Mat4().setToRotation(Vec3f.Z_AXIS, 90.degrees), Mat4())
        val m = Mat4().set(base)
        m.rotate(Vec3f.Z_AXIS, 90.degrees, m)
        assertNear(expected, m)
    }

    @Test
    fun test_rotate_by_quaternion_and_mat3() {
        val q = MutableQuaternion().rotate(50.degrees, Vec3f(1f, 0f, 1f))
        val expected = Mat4().setToTranslate(1f, 2f, 3f).mul(Mat4().setToRotation(q))
        val byQuat = Mat4().setToTranslate(1f, 2f, 3f).apply { rotate(Vec4f(q.x, q.y, q.z, q.w)) }
        assertNear(expected, byQuat)
        val r4 = Mat4().setToRotation(Vec3f(1f, 2f, 3f), 40.degrees)
        val byMat3 = Mat4().setToTranslate(1f, 2f, 3f).apply { rotate(r4.getRotation(Mat3())) }
        assertNear(Mat4().setToTranslate(1f, 2f, 3f).mul(r4), byMat3)
    }

    @Test
    fun test_scale() {
        val m = Mat4().setToTranslate(1f, 2f, 3f).scale(2f, 3f, 4f)
        assertEquals(Vec3f(3f, 5f, 7f), m.transform(MutableVec3f(1f, 1f, 1f)))
        assertEquals(Vec3f(1f, 2f, 3f), m.getTranslation(MutableVec3f()))
        assertNear(Mat4().setToScaling(2f, 2f, 2f), Mat4().scale(2f))
        assertNear(Mat4().setToScaling(2f, 3f, 4f), Mat4().scale(Vec3f(2f, 3f, 4f)))
    }

    @Test
    fun test_get_scale_of_pure_scale() {
        val m = Mat4().setToTranslateAndScaling(1f, 2f, 3f, 2f, -3f, 4f)
        assertEquals(Vec3f(2f, 3f, 4f), m.getScale(MutableVec3f()))
        assertEquals(Vec3f(1f, 2f, 3f), m.getTranslation(MutableVec3f()))
    }

    @Test
    fun test_get_scale_of_uniformly_scaled_rotation() {
        val m = Mat4().setToRotation(Vec3f(1f, 2f, 3f), 40.degrees).scale(3f)
        assertNear(Vec3f(3f, 3f, 3f), m.getScale(MutableVec3f()))
    }

    @Test
    fun test_transform_point_vs_direction() {
        val m = Mat4().setToTranslate(5f, 6f, 7f).rotate(Vec3f.Z_AXIS, 90.degrees)
        assertNear(Vec3f(5f, 7f, 7f), m.transform(MutableVec3f(1f, 0f, 0f)))
        assertNear(Vec3f(0f, 1f, 0f), m.transform(MutableVec3f(1f, 0f, 0f), 0f))
        assertNear(Vec3f(5f, 7f, 7f), m.transform(Vec3f(1f, 0f, 0f), 1f, MutableVec3f()))
        assertNear(Vec3f(5f, 7f, 7f), MutableVec3f(1f, 0f, 0f).mul(m))
    }

    @Test
    fun test_transform_vec4_uses_w() {
        val m = Mat4().setToTranslate(1f, 2f, 3f)
        assertEquals(Vec4f(2f, 3f, 4f, 1f), m.transform(Vec4f(1f, 1f, 1f, 1f), MutableVec4f()))
        assertEquals(Vec4f(1f, 1f, 1f, 0f), m.transform(Vec4f(1f, 1f, 1f, 0f), MutableVec4f()))
    }

    @Test
    fun test_determinant() {
        assertEquals(1f, Mat4().det)
        assertNear(24f, Mat4().setToScaling(2f, 3f, 4f).det)
        assertNear(24f, Mat4().setToTranslate(5f, 6f, 7f).scale(2f, 3f, 4f).det)
        assertNear(-2f, Mat4().setToScaling(-2f, 1f, 1f).det)
        assertNear(1f, Mat4().setToRotation(Vec3f(1f, 2f, 3f), 70.degrees).det)
    }

    @Test
    fun test_determinant_of_product() {
        val a = trs()
        val b = other()
        assertNear(a.det * b.det, a.mul(b, Mat4()).det, 1e-4f)
    }

    @Test
    fun test_determinant_3x3() {
        assertNear(24f, Mat4().setToTranslate(5f, 6f, 7f).scale(2f, 3f, 4f).det3x3)
        assertNear(trs().det, trs().det3x3, 1e-4f)
    }

    @Test
    fun test_set_trs_matches_composed_matrices() {
        val q = MutableQuaternion().rotate(60.degrees, Vec3f(0f, 1f, 1f))
        val expected =
            Mat4()
                .setToTranslate(1f, 2f, 3f)
                .mul(Mat4().setToRotation(q))
                .mul(Mat4().setToScaling(2f, 3f, 4f))
        val actual = Mat4().set(Vec3f(1f, 2f, 3f), Vec4f(q.x, q.y, q.z, q.w), Vec3f(2f, 3f, 4f))
        assertNear(expected, actual)
    }

    @Test
    fun test_set_quaternion_matches_set_to_rotation() {
        val q = MutableQuaternion().rotate(60.degrees, Vec3f(1f, 2f, 3f))
        assertNear(Mat4().setToRotation(q), Mat4().set(q.x, q.y, q.z, q.w))
        assertNear(Mat4().setToRotation(q), Mat4().set(Vec4f(q.x, q.y, q.z, q.w)))
    }

    @Test
    fun test_get_rotation_quaternion_round_trip() {
        for (angle in listOf(30.degrees, 170.degrees, 179.degrees)) {
            for (axis in listOf(Vec3f(1f, 2f, 3f), Vec3f.X_AXIS, Vec3f.Y_AXIS, Vec3f.Z_AXIS)) {
                val q = MutableQuaternion().rotate(angle, axis)
                val m = Mat4().setToRotation(q)
                val back = m.getRotation(MutableQuaternion())
                assertNear(1f, abs(q.dot(back)), 1e-4f)
            }
        }
    }

    @Test
    fun test_set_from_euler_angles_order_is_y_x_z() {
        val expected =
            Mat4()
                .setToRotation(Vec3f.Y_AXIS, 45.degrees)
                .mul(Mat4().setToRotation(Vec3f.X_AXIS, 60.degrees))
                .mul(Mat4().setToRotation(Vec3f.Z_AXIS, 30.degrees))
        assertNear(expected, Mat4().setFromEulerAngles(60.degrees, 45.degrees, 30.degrees))
        assertNear(expected, Mat4().rotate(60.degrees, 45.degrees, 30.degrees))
    }

    @Test
    fun test_set_from_euler_angles_matches_quaternion_set_euler() {
        val q = MutableQuaternion().setEuler(20.degrees, -70.degrees, 110.degrees)
        assertNear(
            Mat4().setToRotation(q),
            Mat4().setFromEulerAngles(20.degrees, -70.degrees, 110.degrees),
        )
    }

    @Test
    fun test_get_scale_of_non_uniform_scaled_rotation() {
        val m =
            Mat4().setToTranslate(1f, 2f, 3f).rotate(Vec3f(1f, 2f, 3f), 40.degrees).scale(2f, 3f, 4f)
        assertNear(Vec3f(2f, 3f, 4f), m.getScale(MutableVec3f()))
        assertNear(4f, m.scaleXSqr)
        assertNear(3f, m.scaleY)
    }

    @Test
    fun test_get_rotation_ignores_scale() {
        val q = MutableQuaternion().rotate(70.degrees, Vec3f(1f, -2f, 0.5f))
        val m = Mat4().setToTranslate(1f, 2f, 3f).mul(Mat4().setToRotation(q)).scale(2f, 0.5f, 3f)
        assertNear(1f, abs(q.dot(m.getRotation(MutableQuaternion()))), 1e-4f)
    }

    @Test
    fun test_get_rotation_with_negative_scale() {
        val q = MutableQuaternion().rotate(50.degrees, Vec3f.Y_AXIS)
        val m = Mat4().setToRotation(q).scale(-2f, 2f, 2f)
        assertNear(1f, abs(q.dot(m.getRotation(MutableQuaternion()))), 1e-4f)
    }

    @Test
    fun test_set_axes_writes_columns() {
        val x = Vec3f(0f, 1f, 0f)
        val y = Vec3f(-1f, 0f, 0f)
        val m = Mat4().set(x, y, Vec3f.Z_AXIS, Vec3f(5f, 6f, 7f))
        assertNear(x, m.transform(Vec3f.X_AXIS, 0f, MutableVec3f()))
        assertNear(y, m.transform(Vec3f.Y_AXIS, 0f, MutableVec3f()))
        assertNear(Vec3f(5f, 7f, 7f), m.transform(Vec3f.X_AXIS, 1f, MutableVec3f()))
    }

    @Test
    fun test_set_to_world_faces_forward() {
        val forward = Vec3f(1f, 0f, 0f)
        val m = Mat4().setToWorld(Vec3f(1f, 2f, 3f), forward, Vec3f.Y_AXIS)
        assertNear(forward, m.transform(Vec3f.NEG_Z_AXIS, 0f, MutableVec3f()))
        assertNear(Vec3f.Y_AXIS, m.transform(Vec3f.Y_AXIS, 0f, MutableVec3f()))
        assertNear(Vec3f(1f, 2f, 3f), m.getTranslation(MutableVec3f()))
    }

    @Test
    fun test_set_to_look_at_direction_maps_direction_to_neg_z() {
        val dir = Vec3f(1f, 0f, -1f)
        val m = Mat4().setToLookAt(dir, Vec3f.UP)
        assertNear(Vec3f(0f, 0f, -sqrt(2f)), m.transform(MutableVec3f(dir), 0f))
        assertNear(Vec3f.Y_AXIS, m.transform(MutableVec3f(Vec3f.UP), 0f))
        assertNear(1f, m.det)
    }

    @Test
    fun test_set_to_look_at_position_target() {
        val pos = Vec3f(1f, 2f, 3f)
        val target = Vec3f(1f, 2f, -7f)
        val m = Mat4().setToLookAt(pos, target, Vec3f.UP)
        assertNear(Vec3f.ZERO, m.transform(MutableVec3f(pos)))
        assertNear(Vec3f(0f, 0f, -10f), m.transform(MutableVec3f(target)))
        assertNear(Vec3f(1f, 0f, 0f), m.transform(MutableVec3f(2f, 2f, 3f)))
        assertNear(Vec3f(0f, 1f, 0f), m.transform(MutableVec3f(1f, 3f, 3f)))
    }

    @Test
    fun test_set_to_look_at_matches_direction_variant_plus_translation() {
        val pos = Vec3f(4f, -1f, 2f)
        val target = Vec3f(0f, 3f, -5f)
        val dir = target.subtract(pos, MutableVec3f())
        val expected = Mat4().setToLookAt(dir, Vec3f.UP).translate(-pos.x, -pos.y, -pos.z)
        assertNear(expected, Mat4().setToLookAt(pos, target, Vec3f.UP))
    }

    @Test
    fun test_rows_and_cols() {
        val m = Mat4()
        m.setRow(0, Vec3f(1f, 2f, 3f), 4f)
        assertEquals(Vec4f(1f, 2f, 3f, 4f), m.getRow(0, MutableVec4f()))
        m.setCol(3, Vec4f(5f, 6f, 7f, 1f))
        assertEquals(Vec4f(5f, 6f, 7f, 1f), m.getCol(3, MutableVec4f()))
        assertEquals(Vec3f(5f, 6f, 7f), m.getOrigin(MutableVec3f()))
        m.setCol(1, Vec3f(8f, 9f, 10f), 0f)
        assertEquals(9f, m.m11)
    }

    @Test
    fun test_get_rotation_transposed() {
        val m = Mat4().setToRotation(Vec3f(1f, 2f, 3f), 40.degrees)
        val r = m.getRotation(Mat3())
        val rt = m.getRotationTransposed(Mat3())
        assertEquals(r.transpose(Mat3()), rt)
    }

    @Test
    fun test_set_to_zero() {
        val m = Mat4().setToZero()
        for (i in 0..15) assertEquals(0f, m[i])
    }

    @Test
    fun test_equals_and_hash_code() {
        assertEquals(trs(), trs())
        assertEquals(trs().hashCode(), trs().hashCode())
        assertNotEquals(trs(), other())
    }

    @Test
    fun test_to_list_and_set_list() {
        val a = trs()
        assertEquals(a, Mat4().set(a.toList()))
    }
}
