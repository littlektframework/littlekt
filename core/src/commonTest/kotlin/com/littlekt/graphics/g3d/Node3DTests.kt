package com.littlekt.graphics.g3d

import com.littlekt.math.Mat4
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.Quaternion
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class Node3DTests {

    private fun assertSameRotation(expected: Quaternion, actual: Quaternion) {
        val dot = abs(expected.dot(actual))
        assertTrue(abs(1f - dot) <= 1e-4f, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-4f) {
        assertTrue(expected.isFuzzyEqual(actual, eps), "expected $expected but was $actual")
    }

    private fun axisAngle(axis: Vec3f, degrees: Float) =
        MutableQuaternion().rotate(degrees.degrees, axis)

    private fun parentAndChild(): Pair<Node3D, Node3D> {
        val parent = Node3D().rotation(axisAngle(Vec3f.Y_AXIS, 90f))
        val child = Node3D()
        parent.addChild(child)
        return parent to child
    }

    @Test
    fun test_global_rotation_multiplies_parent_and_local() {
        val (parent, child) = parentAndChild()
        child.rotation(axisAngle(Vec3f.X_AXIS, 90f))
        val expected = MutableQuaternion().set(parent.globalRotation).mul(child.rotation)
        assertSameRotation(expected, child.globalRotation)
    }

    @Test
    fun test_set_global_rotation_keeps_it_under_a_rotated_parent() {
        val (_, child) = parentAndChild()
        val target = axisAngle(Vec3f(1f, 1f, 0f), 40f)
        child.globalRotation = target
        assertSameRotation(target, child.globalRotation)
        assertSameRotation(axisAngle(Vec3f.Y_AXIS, -90f).mul(target), child.rotation)
    }

    @Test
    fun test_global_rotate_uses_global_axes() {
        val (_, child) = parentAndChild()
        child.globalRotate(x = 90.degrees)
        val expected = axisAngle(Vec3f.X_AXIS, 90f).mul(axisAngle(Vec3f.Y_AXIS, 90f))
        assertSameRotation(expected, child.globalRotation)
    }

    @Test
    fun test_add_child_uses_the_new_parent_transform() {
        val parent = Node3D().position(5f, 0f, 0f)
        val child = Node3D().position(1f, 0f, 0f)
        assertNear(Vec3f(1f, 0f, 0f), child.globalPosition)
        parent.addChild(child)
        assertNear(Vec3f(6f, 0f, 0f), child.globalPosition)
        parent.removeChild(child)
        assertNear(Vec3f(1f, 0f, 0f), child.globalPosition)
    }

    @Test
    fun test_global_translate_moves_in_global_space() {
        val (parent, child) = parentAndChild()
        parent.position(10f, 0f, 0f)
        child.position(0f, 0f, 2f)
        val before = child.globalPosition.toMutableVec3()
        child.globalTranslate(1f, 0f, 0f)
        assertNear(before.add(1f, 0f, 0f), child.globalPosition)
    }

    @Test
    fun test_transform_setter_recovers_rotation_and_scale() {
        val q = axisAngle(Vec3f(1f, 2f, 3f), 35f)
        val node = Node3D()
        node.transform = Mat4().setToTranslate(1f, 2f, 3f).mul(Mat4().setToRotation(q)).scale(2f, 3f, 4f)
        assertSameRotation(q, node.rotation)
        assertNear(Vec3f(2f, 3f, 4f), node.scale)
        assertNear(Vec3f(1f, 2f, 3f), node.position)
    }
}
