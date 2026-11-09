package com.littlekt.graph

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.findFirstNodeOfType
import com.littlekt.graph.node.node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration

class NodeTests {

    private class Target : Node()

    private class Tracking : Node() {
        val added = mutableListOf<Node>()
        val removed = mutableListOf<Node>()
        val descendants = mutableListOf<Node>()
        var updates = 0

        override fun onChildAdded(child: Node) {
            added += child
        }

        override fun onChildRemoved(child: Node) {
            removed += child
        }

        override fun onDescendantAdded(child: Node) {
            descendants += child
            super.onDescendantAdded(child)
        }

        override fun update(dt: Duration) {
            updates++
        }
    }

    private fun Node.childList(): List<Node> = List(childCount) { nodes[it] }

    @Test
    fun test_find_first_node_of_type_searches_all_children() {
        val target = Target()
        val root = Node()
        root.addChild(Node().apply { addChild(Node()) })
        root.addChild(Node().apply { addChild(target) })
        assertSame(target, root.findFirstNodeOfType<Target>())
    }

    @Test
    fun test_find_first_node_of_type_returns_null_when_missing() {
        val root = Node()
        root.addChild(Node())
        assertNull(root.findFirstNodeOfType<Target>())
    }

    @Test
    fun test_hash_code_ignores_name() {
        val node = Node()
        val hash = node.hashCode()
        node.name = "renamed"
        assertEquals(hash, node.hashCode())
        assertNotEquals(Node(), node)
    }

    @Test
    fun test_add_child_sets_parent_and_indexes() {
        val root = Node()
        val a = Node()
        val b = Node()
        root.addChild(a)
        root.addChild(b)
        assertSame(root, a.parent)
        assertEquals(2, root.childCount)
        assertEquals(0, a.index)
        assertEquals(1, b.index)
        assertTrue(root.isNotEmpty())
    }

    @Test
    fun test_add_child_at_inserts_and_reindexes() {
        val root = Node()
        val a = Node()
        val b = Node()
        val c = Node()
        root.addChildren(a, b)
        root.addChildAt(c, 1)
        assertEquals(listOf(a, c, b), root.childList())
        assertEquals(1, c.index)
        assertEquals(2, b.index)
    }

    @Test
    fun test_remove_child_reindexes_siblings() {
        val root = Node()
        val a = Node()
        val b = Node()
        val c = Node()
        root.addChildren(a, b, c)
        root.removeChild(a)
        assertNull(a.parent)
        assertEquals(listOf(b, c), root.childList())
        assertEquals(0, b.index)
        assertEquals(1, c.index)
    }

    @Test
    fun test_remove_child_ignores_non_child() {
        val root = Node()
        val other = Node()
        val a = Node().addTo(other)
        root.removeChild(a)
        assertSame(other, a.parent)
        assertEquals(1, other.childCount)
    }

    @Test
    fun test_remove_child_at_out_of_range_is_ignored() {
        val root = Node()
        root.addChild(Node())
        root.removeChildAt(5)
        assertEquals(1, root.childCount)
        root.removeChildAt(0)
        assertTrue(root.isEmpty())
    }

    @Test
    fun test_plus_and_minus_assign() {
        val root = Node()
        val a = Node()
        root += a
        assertSame(root, a.parent)
        root -= a
        assertNull(a.parent)
        assertEquals(0, root.childCount)
    }

    @Test
    fun test_move_child_reorders_and_reindexes() {
        val root = Node()
        val a = Node()
        val b = Node()
        val c = Node()
        root.addChildren(a, b, c)
        root.moveChild(a, 2)
        assertEquals(listOf(b, c, a), root.childList())
        assertEquals(2, a.index)
        assertEquals(0, b.index)
    }

    @Test
    fun test_move_child_ignores_non_child() {
        val root = Node()
        val a = Node()
        root.addChild(a)
        root.moveChild(Node(), 0)
        assertEquals(listOf(a), root.childList())
    }

    @Test
    fun test_swap_children_swaps_indexes() {
        val root = Node()
        val a = Node()
        val b = Node()
        val c = Node()
        root.addChildren(a, b, c)
        root.swapChildren(a, c)
        assertEquals(listOf(c, b, a), root.childList())
        assertEquals(0, c.index)
        assertEquals(2, a.index)
    }

    @Test
    fun test_swap_children_at_out_of_range_is_ignored() {
        val root = Node()
        val a = Node()
        val b = Node()
        root.addChildren(a, b)
        root.swapChildrenAt(0, 3)
        assertEquals(listOf(a, b), root.childList())
        root.swapChildrenAt(0, 1)
        assertEquals(listOf(b, a), root.childList())
    }

    @Test
    fun test_send_child_to_top_and_bottom() {
        val root = Node()
        val a = Node()
        val b = Node()
        val c = Node()
        root.addChildren(a, b, c)
        root.sendChildToTop(c)
        assertEquals(listOf(c, a, b), root.childList())
        root.sendChildToBottom(c)
        assertEquals(listOf(a, b, c), root.childList())
        root.sendChildAtToBottom(0)
        assertEquals(listOf(b, c, a), root.childList())
        root.sendChildAtToTop(2)
        assertEquals(listOf(a, b, c), root.childList())
        assertEquals(2, c.index)
    }

    @Test
    fun test_parent_setter_reparents() {
        val first = Node()
        val second = Node()
        val a = Node()
        a.parent = first
        a.parent = second
        assertTrue(first.isEmpty())
        assertEquals(listOf(a), second.childList())
        assertSame(second, a.parent)
        a.parent = null
        assertTrue(second.isEmpty())
        assertNull(a.parent)
    }

    @Test
    fun test_add_child_moves_child_from_previous_parent() {
        val first = Node()
        val second = Node()
        val a = Node().addTo(first)
        second.addChild(a)
        assertTrue(first.isEmpty())
        assertEquals(listOf(a), second.childList())
        assertSame(second, a.parent)
    }

    @Test
    fun test_add_child_twice_keeps_index() {
        val root = Node()
        val a = Node()
        val b = Node()
        root.addChildren(a, b)
        root.addChild(a)
        assertEquals(listOf(a, b), root.childList())
        assertEquals(0, a.index)
    }

    @Test
    fun test_child_added_and_removed_callbacks() {
        val root = Tracking()
        val a = Node()
        root.addChild(a)
        root.removeChild(a)
        assertEquals(listOf(a), root.added)
        assertEquals(listOf(a), root.removed)
    }

    @Test
    fun test_descendant_added_reaches_ancestors() {
        val root = Tracking()
        val middle = Node().addTo(root)
        val leaf = Node()
        middle.addChild(leaf)
        assertEquals(listOf(leaf), root.descendants)
    }

    @Test
    fun test_destroy_destroys_children_and_detaches() {
        val root = Node()
        val a = Node().addTo(root)
        val b = Node().addTo(a)
        var destroyed = 0
        a.onDestroy += { destroyed++ }
        b.onDestroy += { destroyed++ }
        a.destroy()
        assertTrue(a.isDestroyed)
        assertTrue(b.isDestroyed)
        assertFalse(a.enabled)
        assertNull(a.parent)
        assertTrue(a.isEmpty())
        assertTrue(root.isEmpty())
        assertEquals(2, destroyed)
        assertTrue(a.onDestroy.isEmpty)
    }

    @Test
    fun test_destroy_all_children() {
        val root = Node()
        val a = Node().addTo(root)
        val b = Node().addTo(root)
        root.destroyAllChildren()
        assertTrue(root.isEmpty())
        assertTrue(a.isDestroyed)
        assertTrue(b.isDestroyed)
        assertFalse(root.isDestroyed)
    }

    @Test
    fun test_enabled_propagates_to_children() {
        val root = Node()
        val a = Node().addTo(root)
        val b = Node().addTo(a)
        root.enabled = false
        assertFalse(a.enabled)
        assertFalse(b.enabled)
        root.enabled = true
        assertTrue(b.enabled)
    }

    @Test
    fun test_enabled_signals_emit_only_on_change() {
        val node = Node()
        var enabled = 0
        var disabled = 0
        node.onEnabled += { enabled++ }
        node.onDisabled += { disabled++ }
        node.enabled = true
        node.enabled = false
        node.enabled = false
        node.enabled = true
        assertEquals(1, enabled)
        assertEquals(1, disabled)
    }

    @Test
    fun test_find_node_by_name_in_direct_children() {
        val root = Node()
        val a = Node().apply { name = "a" }.addTo(root)
        Node().apply { name = "deep" }.addTo(a)
        assertSame(a, root.nodes.findNode("a"))
        assertNull(root.nodes.findNode("deep"))
    }

    @Test
    fun test_nodes_of_type_and_contains() {
        val root = Node()
        val t1 = Target().addTo(root)
        val plain = Node().addTo(root)
        val t2 = Target().addTo(root)
        assertEquals(listOf(t1, t2), root.nodes.nodesOfType<Target>())
        assertEquals(listOf(t1, t2), root.nodes.filterIsInstance<Target>())
        assertTrue(root.nodes.contains(plain))
        assertFalse(root.nodes.contains(Node()))
    }

    @Test
    fun test_node_dsl_adds_child() {
        val root = Node()
        val child = root.node { name = "child" }
        val target = root.node(Target()) { name = "target" }
        assertSame(root, child.parent)
        assertSame(root, target.parent)
        assertEquals(listOf(child, target), root.childList())
    }

    @Test
    fun test_tree_string() {
        val root = Node().apply { name = "root" }
        root.node { name = "a" }.node { name = "b" }
        root.node { name = "c" }
        val expected = "root (Node)\n├── a (Node)\n│   └── b (Node)\n└── c (Node)\n"
        assertEquals(expected, root.treeString().replace("\r\n", "\n"))
    }

    @Test
    fun test_detached_node_defaults() {
        val node = Node()
        assertFalse(node.insideTree)
        assertNull(node.scene)
        assertNull(node.contextOrNull)
        assertEquals(Duration.ZERO, node.dt)
        assertEquals(1f, node.ppu)
        assertEquals(1f, node.fixedProgressionRatio)
        assertFailsWith<IllegalStateException> { node.context }
    }

    @Test
    fun test_update_skips_disabled_children() {
        val root = Node()
        val a = Tracking().addTo(root)
        val b = Tracking().addTo(root)
        b.enabled = false
        root.propagateUpdate()
        assertEquals(1, a.updates)
        assertEquals(0, b.updates)
    }

    @Test
    fun test_update_interval_skips_frames() {
        val root = Node()
        val a = Tracking().addTo(root).apply { updateInterval = 2 }
        repeat(4) { root.propagateUpdate() }
        assertEquals(2, a.updates)
    }

    @Test
    fun test_update_continues_after_child_destroys_itself() {
        val root = Node()
        val a = Node().addTo(root)
        val b = Node().addTo(root)
        val c = Tracking().addTo(root)
        b.onUpdate += { b.destroy() }
        root.propagateUpdate()
        assertEquals(listOf(a, c), root.childList())
        assertEquals(1, c.updates)
    }

    @Test
    fun test_update_signals_emit_with_dt() {
        val node = Node()
        val calls = mutableListOf<String>()
        node.onPreUpdate += { calls += "pre" }
        node.onUpdate += { calls += "update" }
        node.onPostUpdate += { calls += "post" }
        node.onFixedUpdate += { calls += "fixed" }
        node.propagatePreUpdate()
        node.propagateUpdate()
        node.propagatePostUpdate()
        node.propagateFixedUpdate()
        assertEquals(listOf("pre", "update", "post", "fixed"), calls)
    }

    @Test
    fun test_resize_propagates_to_enabled_children() {
        val root = Node()
        val a = Node().addTo(root)
        val b = Node().addTo(root).apply { enabled = false }
        var aSize = 0 to 0
        var bCalls = 0
        a.onResize += { w, h -> aSize = w to h }
        b.onResize += { _, _ -> bCalls++ }
        root.propagateResize(640, 480, false)
        assertEquals(640 to 480, aSize)
        assertEquals(0, bCalls)
    }
}
