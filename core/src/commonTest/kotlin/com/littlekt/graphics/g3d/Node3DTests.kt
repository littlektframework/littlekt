package com.littlekt.graphics.g3d

import com.littlekt.math.Mat4
import com.littlekt.math.Ray
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.Quaternion
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import kotlin.math.abs
import com.littlekt.math.spatial.Capsule
import com.littlekt.math.spatial.SweepHit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.time.Duration
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

    private class BoxNode(private val half: Float = 1f) : Node3D() {
        override fun addToBoundingBox(bounds: BoundingBox) {
            bounds.add(Vec3f(-half, -half, -half)).add(Vec3f(half, half, half))
        }
    }

    private fun ray(ox: Float, oy: Float, oz: Float, dx: Float, dy: Float, dz: Float) =
        Ray().apply {
            origin.set(ox, oy, oz)
            direction.set(dx, dy, dz).norm()
        }

    @Test
    fun test_intersect_ray_uses_transformed_bounds() {
        val node = BoxNode().position(10f, 0f, 0f).scaling(2f)
        node.update(Duration.ZERO)
        val distance = node.intersectRay(ray(0f, 0f, 0f, 1f, 0f, 0f))
        assertTrue(abs(distance - 8f) < 1e-4f, "was $distance")
        assertTrue(node.intersectRay(ray(0f, 3f, 0f, 1f, 0f, 0f)) < 0f)
    }

    @Test
    fun test_intersect_ray_tests_rotated_bounds_tightly() {
        val node = BoxNode().rotation(axisAngle(Vec3f.Y_AXIS, 45f))
        node.update(Duration.ZERO)
        val outside = ray(-9f, 0f, 11f, 1f, 0f, -1f)
        assertTrue(node.intersectRay(outside) < 0f)
        val global = node.globalBounds(BoundingBox())
        assertTrue(outside.intersectBox(global) >= 0f)
    }

    @Test
    fun test_intersect_ray_precise_without_geometry_misses() {
        val parent = Node3D()
        parent.addChild(BoxNode())
        parent.update(Duration.ZERO)
        assertTrue(parent.intersectRay(ray(-10f, 0f, 0f, 1f, 0f, 0f)) >= 0f)
        assertTrue(parent.intersectRay(ray(-10f, 0f, 0f, 1f, 0f, 0f), precise = true) < 0f)
    }

    @Test
    fun test_sweep_sphere_against_bounds() {
        val node = BoxNode().position(10f, 0f, 0f)
        node.update(Duration.ZERO)
        val hit = SweepHit()
        val half = node.globalBounds(BoundingBox()).size.x * 0.5f
        assertTrue(node.sweepSphere(Vec3f(0f, 0f, 0f), 1f, Vec3f(20f, 0f, 0f), hit))
        assertTrue(abs(hit.time - (10f - half - 1f) / 20f) < 1e-4f, "was ${hit.time}")
        assertTrue(abs(hit.normal.x + 1f) < 1e-4f)
        assertFalse(node.sweepSphere(Vec3f(0f, 5f, 0f), 1f, Vec3f(20f, 0f, 0f), hit))
    }

    @Test
    fun test_overlap_capsule_and_precise_without_geometry() {
        val parent = Node3D()
        parent.addChild(BoxNode())
        parent.update(Duration.ZERO)
        val capsule = Capsule(Vec3f(-5f, 0f, 0f), Vec3f(5f, 0f, 0f), 0.2f)
        assertTrue(parent.overlapsCapsule(capsule))
        assertFalse(parent.overlapsCapsule(capsule, precise = true))
        assertFalse(parent.sweepCapsule(capsule, Vec3f(0f, 1f, 0f), SweepHit(), precise = true))
    }
    @Test
    fun test_destroy_detaches_children_and_returns() {
        val root = Node3D()
        val parent = Node3D()
        val child = Node3D()
        val grandChild = Node3D()
        root.addChild(parent)
        parent.addChild(child)
        child.addChild(grandChild)
        parent.addChild(Node3D())
        parent.destroy()
        assertTrue(parent.isDestroyed)
        assertTrue(child.isDestroyed)
        assertTrue(grandChild.isDestroyed)
        assertEquals(0, parent.childCount)
        assertEquals(0, child.childCount)
        assertNull(child.parent)
        assertNull(parent.parent)
        assertEquals(0, root.childCount)
    }

    @Test
    fun test_copy_keeps_local_transform_of_children() {
        val parent = Node3D().position(1f, 0f, 0f)
        val child = Node3D().position(2f, 0f, 0f)
        val grandChild = Node3D().position(0f, 3f, 0f)
        parent.addChild(child)
        child.addChild(grandChild)
        val copy = parent.copy()
        val childCopy = copy.children[0]
        val grandChildCopy = childCopy.children[0]
        assertNear(child.position, childCopy.position)
        assertNear(child.globalPosition, childCopy.globalPosition)
        assertNear(grandChild.globalPosition, grandChildCopy.globalPosition)
    }

    @Test
    fun test_copy_keeps_visible() {
        val parent = Node3D()
        val child = Node3D().apply { visible = false }
        parent.addChild(child)
        val copy = parent.copy()
        assertTrue(copy.visible)
        assertFalse(copy.children[0].visible)
    }

    private class RenderProbe : Node3D() {
        var visits = 0

        override fun forEachRenderedInstance(
            camera: com.littlekt.graphics.Camera?,
            action: (MeshPrimitiveInstance) -> Unit,
        ) {
            if (!visible) return
            visits++
            super.forEachRenderedInstance(camera, action)
        }
    }

    @Test
    fun test_hidden_node_skips_rendering_its_children() {
        val root = Node3D()
        val parent = Node3D()
        val probe = RenderProbe()
        root.addChild(parent)
        parent.addChild(probe)
        root.forEachRenderedInstance(null) {}
        assertEquals(1, probe.visits)
        parent.visible = false
        root.forEachRenderedInstance(null) {}
        assertEquals(1, probe.visits)
        assertEquals(parent, probe.parent)
        parent.visible = true
        root.forEachRenderedInstance(null) {}
        assertEquals(2, probe.visits)
    }
}
