package com.littlekt.graphics

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class CameraTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-3f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_unproject_is_inverse_of_project() {
        val camera = PerspectiveCamera(960, 540)
        camera.near = 0.1f
        camera.far = 100f
        camera.translate(3f, 2f, 10f)
        camera.rotate(20.degrees, Vec3f.Y_AXIS)
        camera.update()

        val world = Vec3f(1f, 0.5f, -4f)
        val ndc = MutableVec3f()
        val result = MutableVec3f()
        assertTrue(camera.project(world, ndc))
        assertTrue(camera.unproject(ndc, result))

        assertNear(world.x, result.x)
        assertNear(world.y, result.y)
        assertNear(world.z, result.z)
    }

    @Test
    fun test_unproject_near_and_far_planes() {
        val camera = PerspectiveCamera(960, 540)
        camera.near = 0.5f
        camera.far = 100f
        camera.update()

        val result = MutableVec3f()
        assertTrue(camera.unproject(Vec3f(0f, 0f, 0f), result))
        assertNear(-0.5f, result.z)
        assertTrue(camera.unproject(Vec3f(0f, 0f, 1f), result))
        assertNear(-100f, result.z, 0.05f)
    }
}
