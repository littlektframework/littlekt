package com.littlekt.graph.node.node2d

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Node2DTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-3f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(x: Float, y: Float, actual: Vec2f) {
        assertNear(x, actual.x)
        assertNear(y, actual.y)
    }

    @Test
    fun test_defaults() {
        val node = Node2D()
        assertNear(0f, 0f, node.position)
        assertNear(0f, 0f, node.globalPosition)
        assertNear(1f, 1f, node.scale)
        assertEquals(Angle.ZERO, node.rotation)
        assertTrue(node.visible)
    }

    @Test
    fun test_root_global_position_equals_position() {
        val node = Node2D()
        node.position(5f, 7f)
        assertNear(5f, 7f, node.globalPosition)
        node.x = 1f
        node.y = 2f
        assertNear(1f, 2f, node.globalPosition)
    }

    @Test
    fun test_child_global_position_adds_parent_translation() {
        val parent = Node2D().apply { position(10f, 20f) }
        val child = Node2D().apply { position(1f, 2f) }.addTo(parent)
        assertNear(11f, 22f, child.globalPosition)
        assertNear(1f, 2f, child.position)
    }

    @Test
    fun test_child_global_position_follows_parent_rotation() {
        val parent = Node2D().apply { rotation = 90.degrees }
        val child = Node2D().apply { position(10f, 0f) }.addTo(parent)
        assertNear(0f, 10f, child.globalPosition)
        assertNear(90f, child.globalRotation.degrees)
    }

    @Test
    fun test_child_global_position_follows_parent_scale() {
        val parent = Node2D().apply { scale(2f, 3f) }
        val child = Node2D().apply { position(5f, 5f) }.addTo(parent)
        assertNear(10f, 15f, child.globalPosition)
        assertNear(2f, 3f, child.globalScale)
    }

    @Test
    fun test_grandchild_combines_transforms() {
        val root = Node2D().apply { position(100f, 0f) }
        val middle = Node2D().apply { rotation = 90.degrees }.addTo(root)
        val leaf = Node2D().apply { position(0f, 10f) }.addTo(middle)
        assertNear(90f, 0f, leaf.globalPosition)
    }

    @Test
    fun test_global_position_updates_after_parent_moves() {
        val parent = Node2D()
        val child = Node2D().apply { position(1f, 1f) }.addTo(parent)
        assertNear(1f, 1f, child.globalPosition)
        parent.position(10f, 10f)
        assertNear(11f, 11f, child.globalPosition)
        parent.rotation = 180.degrees
        assertNear(9f, 9f, child.globalPosition)
    }

    @Test
    fun test_global_rotation_and_scale_combine_with_parent() {
        val parent = Node2D().apply {
            rotation = 30.degrees
            scale(2f, 2f)
        }
        val child = Node2D().apply {
            rotation = 15.degrees
            scale(0.5f, 3f)
        }.addTo(parent)
        assertNear(45f, child.globalRotation.degrees)
        assertNear(1f, 6f, child.globalScale)
    }

    @Test
    fun test_set_global_position_computes_local() {
        val parent = Node2D().apply { position(10f, 10f) }
        val child = Node2D().addTo(parent)
        child.globalPosition(15f, 30f)
        assertNear(5f, 20f, child.position)
        assertNear(15f, 30f, child.globalPosition)
    }

    @Test
    fun test_set_global_position_with_rotated_parent() {
        val parent = Node2D().apply { rotation = 90.degrees }
        val child = Node2D().addTo(parent)
        child.globalPosition = Vec2f(0f, 10f)
        assertNear(10f, 0f, child.position)
    }

    @Test
    fun test_set_global_x_keeps_global_y() {
        val parent = Node2D().apply { position(10f, 10f) }
        val child = Node2D().addTo(parent)
        child.globalX = 50f
        assertNear(50f, 10f, child.globalPosition)
        assertNear(40f, 0f, child.position)
    }

    @Test
    fun test_set_global_y_keeps_global_x() {
        val parent = Node2D().apply { position(10f, 10f) }
        val child = Node2D().addTo(parent)
        child.globalY = 50f
        assertNear(10f, 50f, child.globalPosition)
    }

    @Test
    fun test_set_global_position_to_stale_value_still_applies() {
        val parent = Node2D().apply { position(10f, 10f) }
        val child = Node2D().addTo(parent)
        child.globalPosition(0f, 0f)
        assertNear(-10f, -10f, child.position)
        assertNear(0f, 0f, child.globalPosition)
    }

    @Test
    fun test_set_global_rotation_computes_local() {
        val parent = Node2D().apply { rotation = 30.degrees }
        val child = Node2D().addTo(parent)
        child.globalRotation = 90.degrees
        assertNear(60f, child.rotation.degrees)
        assertNear(90f, child.globalRotation.degrees)
    }

    @Test
    fun test_set_global_scale_computes_local() {
        val parent = Node2D().apply { scale(2f, 4f) }
        val child = Node2D().addTo(parent)
        child.globalScale(4f, 2f)
        assertNear(2f, 0.5f, child.scale)
        assertNear(4f, 2f, child.globalScale)
    }

    @Test
    fun test_global_scale_x_reads_parent_scale() {
        val parent = Node2D().apply { scale(2f, 2f) }
        val child = Node2D().addTo(parent)
        assertNear(2f, child.globalScaleX)
        assertNear(2f, child.globalScaleY)
    }

    @Test
    fun test_translate_adds_offset() {
        val node = Node2D().apply { position(1f, 1f) }
        node.translate(2f, 3f)
        node.translate(Vec2f(1f, 1f))
        assertNear(4f, 5f, node.position)
    }

    @Test
    fun test_to_global_and_to_local_round_trip() {
        val node = Node2D().apply {
            position(10f, 5f)
            rotation = 90.degrees
            scale(2f, 2f)
        }
        val global = node.toGlobal(1f, 0f)
        assertNear(10f, 7f, global)
        val local = node.toLocal(global)
        assertNear(1f, 0f, local)
    }

    @Test
    fun test_to_global_with_out_reuses_vector() {
        val node = Node2D().apply { position(3f, 4f) }
        val out = MutableVec2f()
        val result = node.toGlobal(1f, 1f, out)
        assertTrue(result === out)
        assertNear(4f, 5f, out)
        node.toLocal(4f, 5f, out)
        assertNear(1f, 1f, out)
    }

    @Test
    fun test_reparent_updates_global_position() {
        val first = Node2D().apply { position(10f, 0f) }
        val second = Node2D().apply { position(0f, 20f) }
        val child = Node2D().apply { position(1f, 1f) }.addTo(first)
        assertNear(11f, 1f, child.globalPosition)
        child.parent = second
        assertNear(1f, 21f, child.globalPosition)
    }

    @Test
    fun test_add_and_remove_child_dirties_cached_transform() {
        val parent = Node2D().apply { position(10f, 0f) }
        val child = Node2D().apply { position(1f, 0f) }
        assertNear(1f, 0f, child.globalPosition)
        parent.addChild(child)
        assertNear(11f, 0f, child.globalPosition)
        parent.removeChild(child)
        assertNear(1f, 0f, child.globalPosition)
    }

    @Test
    fun test_reparent_to_plain_node_resets_global_transform() {
        val parent = Node2D().apply {
            position(10f, 10f)
            rotation = 90.degrees
            scale(2f, 2f)
        }
        val child = Node2D().apply { position(1f, 0f) }.addTo(parent)
        assertNear(10f, 12f, child.globalPosition)
        child.parent = Node()
        assertNear(1f, 0f, child.globalPosition)
        assertNear(0f, child.globalRotation.degrees)
        assertNear(1f, 1f, child.globalScale)
        assertNear(2f, 0f, child.toGlobal(1f, 0f))
    }

    @Test
    fun test_transform_propagates_through_plain_node() {
        val root = Node2D().apply { position(5f, 5f) }
        val plain = Node().addTo(root)
        val leaf = Node2D().apply { position(1f, 1f) }.addTo(plain)
        assertNear(1f, 1f, leaf.globalPosition)
        root.position(6f, 6f)
        assertNear(1f, 1f, leaf.globalPosition)
    }

    @Test
    fun test_pivot_must_be_normalized() {
        val node = Node2D()
        node.pivotX = 0.5f
        assertEquals(0.5f, node.pivotX)
        assertFailsWith<IllegalArgumentException> { node.pivotY = 2f }
    }

    @Test
    fun test_visible_propagates_and_emits() {
        val parent = Node2D()
        val child = Node2D().addTo(parent)
        var hidden = 0
        var shown = 0
        child.onInvisible += { hidden++ }
        child.onVisible += { shown++ }
        parent.visible = false
        assertFalse(child.visible)
        parent.visible = false
        parent.visible = true
        assertTrue(child.visible)
        assertEquals(1, hidden)
        assertEquals(1, shown)
    }

    @Test
    fun test_copy_from_copies_transform() {
        val source = Node2D().apply {
            position(3f, 4f)
            rotation = 45.degrees
            scale(2f, 2f)
        }
        val target = Node2D()
        target.copyFrom(source)
        assertNear(3f, 4f, target.position)
        assertNear(45f, target.rotation.degrees)
        assertNear(2f, 2f, target.scale)
        assertNear(3f, 4f, target.globalPosition)
        assertNear(4.4142f, 5.4142f, target.toGlobal(1f, 0f))
    }

    @Test
    fun test_y_sort_orders_sorted_iteration_by_global_y() {
        val root = Node2D().apply { ySort = true }
        val low = Node2D().apply { y = 30f }.addTo(root)
        val high = Node2D().apply { y = 10f }.addTo(root)
        val mid = Node2D().apply { y = 20f }.addTo(root)
        root.propagateUpdate()
        val sorted = mutableListOf<Node>()
        root.nodes.forEachSorted { sorted += it }
        assertEquals(listOf<Node>(high, mid, low), sorted)
        val order = mutableListOf<Node>()
        root.nodes.forEach { order += it }
        assertEquals(listOf<Node>(low, high, mid), order)
    }
}
