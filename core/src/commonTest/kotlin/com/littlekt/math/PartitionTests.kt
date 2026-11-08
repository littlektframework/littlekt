package com.littlekt.math

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartitionTests {

    private fun assertPartitioned(list: List<Int>, k: Int, range: IntRange = list.indices) {
        val pivot = list[k]
        for (i in range.first until k) assertTrue(list[i] <= pivot, "index $i: ${list[i]} > $pivot")
        for (i in k + 1..range.last) assertTrue(list[i] >= pivot, "index $i: ${list[i]} < $pivot")
    }

    @Test
    fun test_partition_small_list_selects_kth() {
        val list = mutableListOf(5, 3, 9, 1, 7, 2, 8)
        list.partition(3) { a, b -> a.compareTo(b) }
        assertEquals(5, list[3])
        assertPartitioned(list, 3)
    }

    @Test
    fun test_partition_every_k() {
        val source = listOf(4, 8, 1, 9, 3, 3, 7, 0, 6, 2, 5, 3)
        val sorted = source.sorted()
        for (k in source.indices) {
            val list = source.toMutableList()
            list.partition(k) { a, b -> a.compareTo(b) }
            assertEquals(sorted[k], list[k])
            assertPartitioned(list, k)
            assertEquals(sorted, list.sorted())
        }
    }

    @Test
    fun test_partition_large_list_uses_sampling() {
        val random = Random(42)
        repeat(5) {
            val source = List(2000) { random.nextInt(-1000, 1000) }
            val k = random.nextInt(source.size)
            val list = source.toMutableList()
            list.partition(k) { a, b -> a.compareTo(b) }
            assertEquals(source.sorted()[k], list[k])
            assertPartitioned(list, k)
        }
    }

    @Test
    fun test_partition_sorted_and_reversed() {
        val asc = MutableList(1000) { it }
        asc.partition(500) { a, b -> a.compareTo(b) }
        assertEquals(500, asc[500])
        val desc = MutableList(1000) { 999 - it }
        desc.partition(10) { a, b -> a.compareTo(b) }
        assertEquals(10, desc[10])
        assertPartitioned(desc, 10)
    }

    @Test
    fun test_partition_all_equal() {
        val list = MutableList(50) { 7 }
        list.partition(25) { a, b -> a.compareTo(b) }
        assertTrue(list.all { it == 7 })
    }

    @Test
    fun test_partition_sub_range() {
        val list = mutableListOf(100, 5, 3, 9, 1, 7, -100)
        list.partition(1..5, 3) { a, b -> a.compareTo(b) }
        assertEquals(100, list[0])
        assertEquals(-100, list[6])
        assertEquals(5, list[3])
        assertPartitioned(list, 3, 1..5)
    }

    @Test
    fun test_partition_array() {
        val array = arrayOf(5, 3, 9, 1, 7, 2, 8)
        array.partition(1) { a, b -> a.compareTo(b) }
        assertEquals(2, array[1])
        assertPartitioned(array.toList(), 1)
    }

    @Test
    fun test_partition_with_custom_comparator() {
        val list = mutableListOf("ccc", "a", "bb", "dddd")
        list.partition(0) { a, b -> b.length.compareTo(a.length) }
        assertEquals("dddd", list[0])
    }
}
