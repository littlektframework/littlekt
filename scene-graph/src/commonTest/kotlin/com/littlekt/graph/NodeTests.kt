package com.littlekt.graph

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.findFirstNodeOfType
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class NodeTests {

    private class Target : Node()

    @Test
    fun test_find_first_node_of_type_searches_all_children() {
        val target = Target()
        val root = Node()
        root.addChild(Node().apply { addChild(Node()) })
        root.addChild(Node().apply { addChild(target) })
        assertSame(target, root.findFirstNodeOfType<Target>())
    }

    @Test
    fun test_hash_code_ignores_name() {
        val node = Node()
        val hash = node.hashCode()
        node.name = "renamed"
        assertSame(hash, node.hashCode())
        assertNotEquals(Node(), node)
    }
}
