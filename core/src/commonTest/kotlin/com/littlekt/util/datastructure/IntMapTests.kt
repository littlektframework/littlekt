package com.littlekt.util.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntMapTests {

    @Test
    fun test_put_get_and_overwrite() {
        val map = IntMap<String>(2)
        repeat(1000) { map[it * 7] = "v$it" }
        assertEquals(1000, map.size)
        repeat(1000) { assertEquals("v$it", map[it * 7]) }
        assertNull(map[3])

        map[7] = "updated"
        assertEquals(1000, map.size)
        assertEquals("updated", map[7])
    }

    @Test
    fun test_negative_and_zero_keys() {
        val map = IntMap<String>()
        map[0] = "zero"
        map[-5] = "neg"
        assertEquals("zero", map[0])
        assertEquals("neg", map[-5])
    }

    @Test
    fun test_get_or_put_and_clear() {
        val map = IntMap<String>()
        assertEquals("a", map.getOrPut(1) { "a" })
        assertEquals("a", map.getOrPut(1) { "b" })
        var count = 0
        map.forEachValue { count++ }
        assertEquals(1, count)
        map.clear()
        assertEquals(0, map.size)
        assertNull(map[1])
    }

    @Test
    fun test_zero_initial_capacity_grows() {
        val map = IntMap<String>(0)
        repeat(10) { map[it] = "v$it" }
        assertEquals(10, map.size)
        repeat(10) { assertEquals("v$it", map[it]) }
    }

    @Test
    fun test_extreme_keys() {
        val map = IntMap<String>()
        map[Int.MIN_VALUE] = "min"
        map[Int.MAX_VALUE] = "max"
        assertEquals("min", map[Int.MIN_VALUE])
        assertEquals("max", map[Int.MAX_VALUE])
        assertEquals(2, map.size)
    }

    @Test
    fun test_colliding_keys() {
        val map = IntMap<Int>(4)
        val keys = IntArray(64) { it shl 16 }
        keys.forEach { map[it] = it }
        keys.forEach { assertEquals(it, map[it]) }
        assertNull(map[1])
    }

    @Test
    fun test_for_each_value_after_resize() {
        val map = IntMap<Int>(2)
        repeat(50) { map[it] = it }
        var sum = 0
        map.forEachValue { sum += it }
        assertEquals((0 until 50).sum(), sum)
    }

    @Test
    fun test_clear_and_reuse() {
        val map = IntMap<String>()
        repeat(20) { map[it] = "a$it" }
        map.clear()
        var count = 0
        map.forEachValue { count++ }
        assertEquals(0, count)
        map[3] = "b"
        assertEquals(1, map.size)
        assertEquals("b", map[3])
        assertNull(map[4])
    }

    @Test
    fun test_remove_missing_returns_null() {
        val map = IntMap<String>()
        map[1] = "a"
        assertNull(map.remove(2))
        assertEquals(1, map.size)
    }

    @Test
    fun test_remove_keeps_colliding_keys() {
        val map = IntMap<Int>(4)
        val keys = IntArray(64) { it shl 16 }
        keys.forEach { map[it] = it }
        for (i in keys.indices step 2) assertEquals(keys[i], map.remove(keys[i]))
        assertEquals(32, map.size)
        keys.forEachIndexed { i, key ->
            if (i % 2 == 0) assertNull(map[key]) else assertEquals(key, map[key])
        }
    }

    @Test
    fun test_remove_and_reinsert() {
        val map = IntMap<String>(2)
        repeat(100) { map[it] = "v$it" }
        repeat(100) { map.remove(it) }
        assertEquals(0, map.size)
        repeat(100) { assertNull(map[it]) }
        map[5] = "b"
        assertEquals("b", map[5])
    }

    @Test
    fun test_get_or_put_invokes_default_once() {
        val map = IntMap<String>()
        var calls = 0
        map.getOrPut(7) { calls++; "x" }
        map.getOrPut(7) { calls++; "y" }
        assertEquals(1, calls)
        assertEquals("x", map[7])
    }
}
