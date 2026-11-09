package com.littlekt.util.datastructure

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TreeMapTests {

    private fun mapOf10_20_30(): TreeMap<Int, String> {
        val map = TreeMap<Int, String>()
        map[20] = "b"
        map[10] = "a"
        map[30] = "c"
        return map
    }

    @Test
    fun test_put_get_and_overwrite() {
        val map = TreeMap<Int, String>()
        assertNull(map.put(1, "a"))
        assertEquals("a", map.put(1, "b"))
        assertEquals("b", map[1])
        assertEquals(1, map.size)
        assertNull(map[2])
    }

    @Test
    fun test_keys_are_sorted() {
        val map = TreeMap<Int, Int>()
        listOf(5, 1, 4, 2, 3).forEach { map[it] = it * 10 }
        assertEquals(listOf(1, 2, 3, 4, 5), map.keys.toList())
        assertEquals(listOf(10, 20, 30, 40, 50), map.values.toList())
        assertEquals(listOf(1, 2, 3, 4, 5), map.entries.map { it.key })
    }

    @Test
    fun test_values_keeps_duplicates() {
        val map = TreeMap<Int, String>()
        map[1] = "x"
        map[2] = "x"
        map[3] = "y"
        assertEquals(listOf("x", "x", "y"), map.values.toList())
        assertEquals(map.size, map.values.size)
    }

    @Test
    fun test_first_and_last() {
        val map = mapOf10_20_30()
        assertEquals(10, map.firstKey())
        assertEquals("a", map.firstValue())
        assertEquals(30, map.lastKey())
        assertEquals("c", map.lastValue())
    }

    @Test
    fun test_first_and_last_empty_throws() {
        val map = TreeMap<Int, String>()
        assertFailsWith<NoSuchElementException> { map.firstEntry() }
        assertFailsWith<NoSuchElementException> { map.lastEntry() }
    }

    @Test
    fun test_ceiling() {
        val map = mapOf10_20_30()
        assertEquals(10, map.ceilingKey(5))
        assertEquals(20, map.ceilingKey(15))
        assertEquals(20, map.ceilingKey(20))
        assertEquals("c", map.ceilingValue(25))
        assertNull(map.ceilingKey(31))
    }

    @Test
    fun test_floor() {
        val map = mapOf10_20_30()
        assertNull(map.floorKey(5))
        assertEquals(10, map.floorKey(15))
        assertEquals(20, map.floorKey(20))
        assertEquals("c", map.floorValue(35))
    }

    @Test
    fun test_higher() {
        val map = mapOf10_20_30()
        assertEquals(10, map.higherKey(5))
        assertEquals(30, map.higherKey(20))
        assertEquals("b", map.higherValue(10))
        assertNull(map.higherKey(30))
    }

    @Test
    fun test_lower() {
        val map = mapOf10_20_30()
        assertNull(map.lowerKey(10))
        assertEquals(10, map.lowerKey(20))
        assertEquals("b", map.lowerValue(25))
        assertEquals(30, map.lowerKey(35))
    }

    @Test
    fun test_navigation_on_empty_is_null() {
        val map = TreeMap<Int, String>()
        assertNull(map.ceilingEntry(1))
        assertNull(map.floorEntry(1))
        assertNull(map.higherEntry(1))
        assertNull(map.lowerEntry(1))
    }

    @Test
    fun test_remove() {
        val map = mapOf10_20_30()
        assertEquals("b", map.remove(20))
        assertNull(map.remove(20))
        assertEquals(2, map.size)
        assertFalse(map.containsKey(20))
        assertEquals(listOf(10, 30), map.keys.toList())
    }

    @Test
    fun test_remove_only_entry() {
        val map = TreeMap<Int, String>()
        map[1] = "a"
        map.remove(1)
        assertTrue(map.isEmpty())
        assertTrue(map.keys.isEmpty())
        map[2] = "b"
        assertEquals(2, map.firstKey())
    }

    @Test
    fun test_random_insert_remove_matches_reference() {
        val random = Random(42)
        val map = TreeMap<Int, Int>()
        val reference = mutableMapOf<Int, Int>()
        repeat(2000) {
            val key = random.nextInt(300)
            if (random.nextInt(3) == 0) {
                assertEquals(reference.remove(key), map.remove(key))
            } else {
                assertEquals(reference.put(key, it), map.put(key, it))
            }
            assertEquals(reference.size, map.size)
        }
        val sorted = reference.keys.sorted()
        assertEquals(sorted, map.keys.toList())
        assertEquals(sorted.map { reference[it] }, map.values.toList())
        sorted.forEach { assertEquals(reference[it], map[it]) }
        assertEquals(sorted.first(), map.firstKey())
        assertEquals(sorted.last(), map.lastKey())
    }

    @Test
    fun test_contains_value_put_all_and_clear() {
        val map = TreeMap<Int, String>()
        map.putAll(mapOf(2 to "b", 1 to "a"))
        assertTrue(map.containsValue("a"))
        assertFalse(map.containsValue("z"))
        assertTrue(map.containsKey(2))
        map.clear()
        assertTrue(map.isEmpty())
        assertNull(map[1])
    }

    @Test
    fun test_entry_set_value_updates_map() {
        val map = mapOf10_20_30()
        map.firstEntry().setValue("z")
        assertEquals("z", map[10])
    }

    @Test
    fun test_string_keys() {
        val map = TreeMap<String, Int>()
        map["b"] = 2
        map["a"] = 1
        map["c"] = 3
        assertEquals(listOf("a", "b", "c"), map.keys.toList())
        assertEquals("b", map.ceilingKey("aa"))
    }
}
