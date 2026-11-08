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
}
