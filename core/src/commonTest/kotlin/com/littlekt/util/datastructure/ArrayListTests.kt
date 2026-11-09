package com.littlekt.util.datastructure

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrayListTests {

    @Test
    fun test_add_and_get() {
        val list = IntArrayList()
        list.add(1)
        list.add(2, 3)
        list += 4
        assertEquals(4, list.size)
        assertEquals(listOf(1, 2, 3, 4), (0 until list.size).map { list[it] })
    }

    @Test
    fun test_growth_keeps_values() {
        val list = IntArrayList(2)
        repeat(100) { list.add(it) }
        assertEquals(100, list.size)
        repeat(100) { assertEquals(it, list[it]) }
    }

    @Test
    fun test_bulk_add_does_not_over_allocate() {
        val list = IntArrayList(4)
        list.add(intArrayOf(1, 2))
        list.add(intArrayOf(3, 4))
        assertEquals(4, list.capacity)
        assertContentEquals(intArrayOf(1, 2, 3, 4), list.toIntArray())
    }

    @Test
    fun test_size_setter_grows_only_when_needed() {
        val list = IntArrayList(4)
        list.add(1, 2, 3)
        list.size = 4
        assertEquals(4, list.capacity)
        list.size = 10
        assertEquals(10, list.size)
        assertTrue(list.capacity >= 10)
    }

    @Test
    fun test_size_setter_shrinks() {
        val list = intArrayListOf(1, 2, 3, 4)
        list.size = 2
        assertEquals(2, list.size)
        assertFalse(list.contains(3))
    }

    @Test
    fun test_clear_reuses_capacity() {
        val list = IntArrayList(4)
        list.add(1, 2, 3, 4)
        val data = list.data
        list.clear()
        assertTrue(list.isEmpty())
        list.add(5, 6, 7, 8)
        assertTrue(data === list.data)
        assertEquals(5, list[0])
    }

    @Test
    fun test_set_beyond_size_extends() {
        val list = IntArrayList()
        list[3] = 9
        assertEquals(4, list.size)
        assertEquals(9, list[3])
    }

    @Test
    fun test_get_or_null_out_of_range() {
        val list = IntArrayList(8)
        list.add(1, 2)
        assertEquals(2, list.getOrNull(1))
        assertNull(list.getOrNull(2))
        assertNull(list.getOrNull(-1))
        assertNull(list.getOrNull(100))
    }

    @Test
    fun test_get_or_null_after_clear() {
        val list = floatArrayListOf(1f, 2f)
        list.clear()
        assertNull(list.getOrNull(0))
    }

    @Test
    fun test_remove_at_shifts() {
        val list = intArrayListOf(1, 2, 3, 4)
        assertEquals(2, list.removeAt(1))
        assertContentEquals(intArrayOf(1, 3, 4), list.toIntArray())
    }

    @Test
    fun test_remove_at_range() {
        val list = intArrayListOf(1, 2, 3, 4, 5)
        assertEquals(2, list.removeAt(1, 3))
        assertContentEquals(intArrayOf(1, 5), list.toIntArray())
    }

    @Test
    fun test_remove_last() {
        val list = intArrayListOf(1, 2, 3)
        assertEquals(3, list.removeAt(2))
        assertContentEquals(intArrayOf(1, 2), list.toIntArray())
    }

    @Test
    fun test_remove_at_out_of_range_throws() {
        val list = intArrayListOf(1, 2)
        assertFailsWith<IndexOutOfBoundsException> { list.removeAt(2) }
        assertFailsWith<IndexOutOfBoundsException> { list.removeAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { list.removeAt(1, 2) }
    }

    @Test
    fun test_insert_at() {
        val list = intArrayListOf(1, 3)
        list.insertAt(1, 2)
        list.insertAt(0, 0)
        assertContentEquals(intArrayOf(0, 1, 2, 3), list.toIntArray())
    }

    @Test
    fun test_insert_array_at() {
        val list = intArrayListOf(1, 5)
        list.insertAt(1, intArrayOf(9, 2, 3, 4, 9), 1, 4)
        assertContentEquals(intArrayOf(1, 2, 3, 4, 5), list.toIntArray())
    }

    @Test
    fun test_insert_into_empty() {
        val list = IntArrayList()
        list.insertAt(0, 7)
        assertContentEquals(intArrayOf(7), list.toIntArray())
    }

    @Test
    fun test_index_of_and_last_index_of() {
        val list = intArrayListOf(1, 2, 1, 3)
        assertEquals(0, list.indexOf(1))
        assertEquals(2, list.lastIndexOf(1))
        assertEquals(-1, list.indexOf(9))
        assertEquals(2, list.indexOf(1, start = 1))
    }

    @Test
    fun test_contains_ignores_unused_capacity() {
        val list = IntArrayList(8)
        list.add(1)
        assertTrue(list.contains(1))
        assertFalse(list.contains(0))
        assertTrue(list.containsAll(listOf(1)))
        assertFalse(list.containsAll(listOf(1, 0)))
    }

    @Test
    fun test_pop() {
        val list = intArrayListOf(1, 2)
        assertEquals(2, list.pop())
        assertEquals(1, list.size)
    }

    @Test
    fun test_swap() {
        val list = intArrayListOf(1, 2, 3)
        list.swap(0, 2)
        assertContentEquals(intArrayOf(3, 2, 1), list.toIntArray())
    }

    @Test
    fun test_equals_and_hash_code() {
        val a = intArrayListOf(1, 2, 3)
        val b = IntArrayList(20)
        b.add(1, 2, 3)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        b.add(4)
        assertNotEquals(a, b)
    }

    @Test
    fun test_to_string() {
        assertEquals("[1, 2, 3]", intArrayListOf(1, 2, 3).toString())
        assertEquals("[]", IntArrayList().toString())
    }

    @Test
    fun test_iterator_only_covers_size() {
        val list = IntArrayList(8)
        list.add(1, 2)
        assertEquals(listOf(1, 2), list.toList())
    }

    @Test
    fun test_sub_list() {
        val list = intArrayListOf(1, 2, 3, 4)
        assertEquals(listOf(2, 3), list.subList(1, 3))
    }

    @Test
    fun test_sub_list_beyond_size_throws() {
        val list = IntArrayList(8)
        list.add(1, 2)
        assertFailsWith<IndexOutOfBoundsException> { list.subList(0, 4) }
    }

    @Test
    fun test_should_grow_false_throws_when_full() {
        val list = IntArrayList(2, shouldGrow = false)
        list.add(1, 2)
        assertFailsWith<IndexOutOfBoundsException> { list.add(3) }
    }

    @Test
    fun test_copy_constructor_is_independent() {
        val a = intArrayListOf(1, 2)
        val b = IntArrayList(a)
        b[0] = 5
        assertEquals(1, a[0])
        assertEquals(5, b[0])
    }

    @Test
    fun test_add_array_with_offset() {
        val list = IntArrayList()
        list.add(intArrayOf(1, 2, 3, 4), offset = 1, length = 2)
        assertContentEquals(intArrayOf(2, 3), list.toIntArray())
    }

    @Test
    fun test_add_other_list_and_iterable() {
        val list = intArrayListOf(1)
        list += intArrayListOf(2, 3)
        list += listOf(4)
        assertContentEquals(intArrayOf(1, 2, 3, 4), list.toIntArray())
    }

    @Test
    fun test_int_to_short_array() {
        assertContentEquals(shortArrayOf(1, 2), intArrayListOf(1, 2).toShortArray())
    }

    @Test
    fun test_float_array_list() {
        val list = FloatArrayList(1)
        list.add(1f)
        list += floatArrayOf(2f, 3f)
        assertContentEquals(floatArrayOf(1f, 2f, 3f), list.toFloatArray())
        assertEquals(1, list.indexOf(2f))
        assertEquals(3f, list.removeAt(2))
        assertEquals(floatArrayListOf(1f, 2f), list)
    }

    @Test
    fun test_float_bulk_add_does_not_over_allocate() {
        val list = FloatArrayList(4)
        list.add(floatArrayOf(1f, 2f))
        list.add(floatArrayOf(3f, 4f))
        assertEquals(4, list.capacity)
    }

    @Test
    fun test_double_array_list() {
        val list = doubleArrayListOf(1.0, 2.0)
        list.insertAt(1, 1.5)
        assertContentEquals(doubleArrayOf(1.0, 1.5, 2.0), list.toDoubleArray())
        assertNull(list.getOrNull(3))
    }

    @Test
    fun test_short_array_list() {
        val list = shortArrayListOf(1, 2)
        list.add(3.toShort())
        assertContentEquals(shortArrayOf(1, 2, 3), list.toShortArray())
        assertNull(list.getOrNull(3))
        assertEquals(1, list.lastIndexOf(2.toShort()))
    }

    @Test
    fun test_array_to_list_extensions() {
        assertEquals(intArrayListOf(1, 2), intArrayOf(1, 2).toIntArrayList())
        assertEquals(floatArrayListOf(1f), floatArrayOf(1f).toFloatArrayList())
        assertEquals(shortArrayListOf(1), shortArrayOf(1).toShortArrayList())
    }
}
