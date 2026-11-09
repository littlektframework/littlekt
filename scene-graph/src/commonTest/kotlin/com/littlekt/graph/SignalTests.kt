package com.littlekt.graph

import com.littlekt.graph.node.Node
import com.littlekt.graph.util.signal
import com.littlekt.graph.util.signal1v
import com.littlekt.graph.util.signal2v
import com.littlekt.graph.util.signal3v
import com.littlekt.graph.util.signal4v
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun test_is_empty_tracks_slots_and_connections() {
        val signal = signal()
        assertTrue(signal.isEmpty)
        val slot: () -> Unit = {}
        signal += slot
        assertFalse(signal.isEmpty)
        signal -= slot
        assertTrue(signal.isEmpty)
        signal.connect(this) {}
        assertFalse(signal.isEmpty)
    }

    @Test
    fun test_minus_assign_removes_only_that_slot() {
        val signal = signal()
        val calls = mutableListOf<String>()
        val a: () -> Unit = { calls += "a" }
        val b: () -> Unit = { calls += "b" }
        signal += a
        signal += b
        signal -= a
        signal.emit()
        assertEquals(listOf("b"), calls)
    }

    @Test
    fun test_disconnect_unknown_is_ignored() {
        val signal = signal()
        var calls = 0
        signal.connect(this) { calls++ }
        signal.disconnect(Node())
        signal.emit()
        assertEquals(1, calls)
    }

    @Test
    fun test_connected_slots_emit_before_plain_slots() {
        val signal = signal()
        val calls = mutableListOf<String>()
        signal += { calls += "slot" }
        signal.connect(this) { calls += "connected" }
        signal.emit()
        assertEquals(listOf("connected", "slot"), calls)
    }

    @Test
    fun test_clear_removes_everything() {
        val signal = signal1v<Int>()
        var calls = 0
        signal += { calls++ }
        signal.connect(this) { calls++ }
        signal.clear()
        signal.emit(1)
        assertEquals(0, calls)
        assertTrue(signal.isEmpty)
    }

    @Test
    fun test_slot_removing_itself_during_emit_does_not_skip_next() {
        val signal = signal()
        val calls = mutableListOf<String>()
        lateinit var once: () -> Unit
        once = {
            calls += "once"
            signal -= once
        }
        signal += once
        signal += { calls += "after" }
        signal.emit()
        signal.emit()
        assertEquals(listOf("once", "after", "after"), calls)
    }

    @Test
    fun test_connection_disconnecting_during_emit_does_not_skip_next() {
        val signal = signal()
        val first = Node()
        val second = Node()
        val calls = mutableListOf<String>()
        signal.connect(first) {
            calls += "first"
            signal.disconnect(first)
        }
        signal.connect(second) { calls += "second" }
        signal.emit()
        assertEquals(listOf("first", "second"), calls)
    }

    @Test
    fun test_multi_parameter_signals() {
        val double = signal2v<Int, Int>()
        val triple = signal3v<Int, Int, Int>()
        val quad = signal4v<Int, Int, Int, Int>()
        var total = 0
        double += { a, b -> total += a + b }
        triple.connect(this) { a, b, c -> total += a * b * c }
        quad += { a, b, c, d -> total += a + b + c + d }
        double.emit(1, 2)
        triple.emit(2, 3, 4)
        quad.emit(1, 1, 1, 1)
        assertEquals(31, total)
    }
}
