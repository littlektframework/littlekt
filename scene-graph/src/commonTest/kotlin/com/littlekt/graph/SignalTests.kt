package com.littlekt.graph

import com.littlekt.graph.node.Node
import com.littlekt.graph.util.signal
import com.littlekt.graph.util.signal1v
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalTests {

    @Test
    fun test_emit_connections_and_slots() {
        val signal = signal1v<Int>()
        var total = 0
        signal += { total += it }
        signal.connect(this) { total += it * 10 }
        signal.emit(2)
        assertEquals(22, total)
    }

    @Test
    fun test_invoke_adds_slot() {
        val signal = signal()
        var calls = 0
        signal { calls++ }
        signal.emit()
        assertEquals(1, calls)
    }

    @Test
    fun test_connect_replaces_by_identity() {
        val signal = signal()
        val node = Node()
        var calls = 0
        signal.connect(node) { calls += 1 }
        signal.connect(node) { calls += 10 }
        signal.emit()
        assertEquals(10, calls)
    }

    @Test
    fun test_disconnect_after_rename() {
        val signal = signal()
        val node = Node().apply { name = "before" }
        var calls = 0
        signal.connect(node) { calls++ }
        node.name = "after"
        signal.disconnect(node)
        signal.emit()
        assertEquals(0, calls)
        assertTrue(signal.isEmpty)
    }
}
