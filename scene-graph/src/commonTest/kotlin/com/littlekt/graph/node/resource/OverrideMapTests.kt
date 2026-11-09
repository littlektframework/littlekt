package com.littlekt.graph.node.resource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OverrideMapTests {

    @Test
    fun test_put_and_remove_notify() {
        var calls = 0
        val map = OverrideMap<String, Int> { calls++ }
        assertNull(map.put("a", 1))
        assertEquals(1, map.put("a", 2))
        assertEquals(2, map.remove("a"))
        assertEquals(3, calls)
    }

    @Test
    fun test_put_all_notifies_once() {
        var calls = 0
        val map = OverrideMap<String, Int> { calls++ }
        map.putAll(mapOf("a" to 1, "b" to 2))
        assertEquals(1, calls)
        assertEquals(2, map.size)
        assertEquals(2, map["b"])
    }

    @Test
    fun test_set_operator_notifies() {
        var calls = 0
        val map = OverrideMap<String, Int> { calls++ }
        map["a"] = 1
        assertEquals(1, calls)
        assertTrue(map.containsKey("a"))
        assertTrue(map.containsValue(1))
    }

    @Test
    fun test_clear_empties_without_callback() {
        var calls = 0
        val map = OverrideMap<String, Int> { calls++ }
        map["a"] = 1
        map.clear()
        assertTrue(map.isEmpty())
        assertEquals(1, calls)
    }

    @Test
    fun test_works_without_callback() {
        val map = OverrideMap<String, Int>()
        map["a"] = 1
        assertEquals(setOf("a"), map.keys)
        assertEquals(listOf(1), map.values.toList())
    }
}
