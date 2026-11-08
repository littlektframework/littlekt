package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class Mat4Tests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun Mat4.clip(x: Float, y: Float, z: Float): MutableVec4f =
        transform(MutableVec4f(x, y, z, 1f))

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
    fun test_perspective_maps_near_far_to_zero_one() {
        val proj = Mat4().setToPerspective(60.degrees, 1f, 0.1f, 100f)
        val near = proj.clip(0f, 0f, -0.1f)
        val far = proj.clip(0f, 0f, -100f)

        assertNear(0f, near.z / near.w)
        assertNear(1f, far.z / far.w, 1e-4f)
    }
}
