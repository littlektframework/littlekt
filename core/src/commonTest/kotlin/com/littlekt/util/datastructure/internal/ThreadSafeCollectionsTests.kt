package com.littlekt.util.datastructure.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThreadSafeCollectionsTests {

    @Test
    fun test_list_add_get_remove() {
        val list = threadSafeMutableListOf<Int>()
        list.add(1)
        list.add(0, 0)
        list.addAll(listOf(2, 3))
        assertEquals(listOf(0, 1, 2, 3), list.toList())
        assertEquals(2, list[2])
        assertTrue(list.remove(0))
        assertEquals(1, list.removeAt(0))
        assertEquals(2, list.size)
    }

    @Test
    fun test_list_vararg_factory_and_lookups() {
        val list = threadSafeMutableListOf(1, 2, 1)
        assertEquals(0, list.indexOf(1))
        assertEquals(2, list.lastIndexOf(1))
        assertTrue(list.contains(2))
        assertTrue(list.containsAll(listOf(1, 2)))
        assertFalse(list.isEmpty())
    }

    @Test
    fun test_list_iterator_remove() {
        val list = threadSafeMutableListOf(1, 2, 3, 4)
        val it = list.iterator()
        while (it.hasNext()) if (it.next() % 2 == 0) it.remove()
        assertEquals(listOf(1, 3), list.toList())
    }

    @Test
    fun test_list_iterator_set_and_add() {
        val list = threadSafeMutableListOf(1, 2)
        val it = list.listIterator()
        it.next()
        it.set(5)
        it.add(6)
        assertEquals(listOf(5, 6, 2), list.toList())
        assertTrue(it.hasPrevious())
        assertEquals(2, it.nextIndex())
    }

    @Test
    fun test_list_set_retain_remove_all_and_clear() {
        val list = threadSafeMutableListOf(1, 2, 3, 4)
        assertEquals(1, list.set(0, 9))
        list.retainAll(listOf(9, 2, 3))
        list.removeAll(listOf(3))
        assertEquals(listOf(9, 2), list.toList())
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun test_list_sub_list_writes_through() {
        val list = threadSafeMutableListOf(1, 2, 3, 4)
        list.subList(1, 3).clear()
        assertEquals(listOf(1, 4), list.toList())
    }

    @Test
    fun test_map_put_get_remove() {
        val map = threadSafeMutableMapOf<String, Int>()
        assertNull(map.put("a", 1))
        assertEquals(1, map.put("a", 2))
        assertEquals(2, map["a"])
        assertTrue(map.containsKey("a"))
        assertTrue(map.containsValue(2))
        assertEquals(2, map.remove("a"))
        assertTrue(map.isEmpty())
    }

    @Test
    fun test_map_vararg_factory_and_put_all() {
        val map = threadSafeMutableMapOf("a" to 1)
        map.putAll(mapOf("b" to 2, "c" to 3))
        assertEquals(3, map.size)
        assertEquals(setOf("a", "b", "c"), map.keys)
        assertEquals(listOf(1, 2, 3), map.values.sorted())
        map.clear()
        assertEquals(0, map.size)
    }

    @Test
    fun test_map_entries_snapshot() {
        val map = threadSafeMutableMapOf("a" to 1, "b" to 2)
        val entries = map.entries
        map["c"] = 3
        assertEquals(2, entries.size)
        assertEquals(setOf("a" to 1, "b" to 2), entries.map { it.key to it.value }.toSet())
    }

    @Test
    fun test_map_entry_set_value_throws() {
        val map = threadSafeMutableMapOf("a" to 1)
        assertFailsWith<UnsupportedOperationException> { map.entries.first().setValue(2) }
    }
}
