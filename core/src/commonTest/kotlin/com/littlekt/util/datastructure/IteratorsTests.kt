package com.littlekt.util.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IteratorsTests {

    @Test
    fun test_fast_for_each_list_and_array() {
        val out = mutableListOf<Int>()
        listOf(1, 2, 3).fastForEach { out += it }
        arrayOf(4, 5).fastForEach { out += it }
        assertEquals(listOf(1, 2, 3, 4, 5), out)
    }

    @Test
    fun test_fast_for_each_primitive_lists() {
        var intSum = 0
        intArrayListOf(1, 2, 3).fastForEach { intSum += it }
        var floatSum = 0f
        floatArrayListOf(1f, 2f).fastForEach { floatSum += it }
        var doubleSum = 0.0
        doubleArrayListOf(0.5, 0.25).fastForEach { doubleSum += it }
        assertEquals(6, intSum)
        assertEquals(3f, floatSum)
        assertEquals(0.75, doubleSum)
    }

    @Test
    fun test_fast_for_each_only_covers_size() {
        val list = IntArrayList(8)
        list.add(1, 2)
        var count = 0
        list.fastForEach { count++ }
        assertEquals(2, count)
    }

    @Test
    fun test_fast_for_each_with_index() {
        val out = mutableListOf<String>()
        listOf("a", "b").fastForEachWithIndex { i, v -> out += "$i$v" }
        arrayOf("c").fastForEachWithIndex { i, v -> out += "$i$v" }
        intArrayListOf(7).fastForEachWithIndex { i, v -> out += "$i$v" }
        floatArrayListOf(1f).fastForEachWithIndex { i, v -> out += "$i${v.toInt()}" }
        doubleArrayListOf(2.0).fastForEachWithIndex { i, v -> out += "$i${v.toInt()}" }
        assertEquals(listOf("0a", "1b", "0c", "07", "01", "02"), out)
    }

    @Test
    fun test_reversed_iteration() {
        val a = mutableListOf<Int>()
        val b = mutableListOf<Int>()
        listOf(1, 2, 3).forEachReversed { a += it }
        listOf(1, 2, 3).fastForEachReverse { b += it }
        assertEquals(listOf(3, 2, 1), a)
        assertEquals(listOf(3, 2, 1), b)
    }

    @Test
    fun test_reversed_iteration_empty() {
        var count = 0
        emptyList<Int>().forEachReversed { count++ }
        emptyList<Int>().fastForEachReverse { count++ }
        assertEquals(0, count)
    }

    @Test
    fun test_fast_iterate_remove_keeps_order() {
        val list = mutableListOf(1, 2, 3, 4, 5, 6)
        val result = list.fastIterateRemove { it % 2 == 0 }
        assertSame(list, result)
        assertEquals(listOf(1, 3, 5), list)
    }

    @Test
    fun test_fast_iterate_remove_first_and_last() {
        val list = mutableListOf(1, 2, 3, 4)
        list.fastIterateRemove { it == 1 || it == 4 }
        assertEquals(listOf(2, 3), list)
    }

    @Test
    fun test_fast_iterate_remove_all_and_none() {
        val all = mutableListOf(1, 2, 3)
        all.fastIterateRemove { true }
        assertEquals(emptyList(), all)
        val none = mutableListOf(1, 2, 3)
        none.fastIterateRemove { false }
        assertEquals(listOf(1, 2, 3), none)
    }

    @Test
    fun test_fast_iterate_remove_visits_each_once() {
        val visited = mutableListOf<Int>()
        mutableListOf(1, 2, 3, 4).fastIterateRemove {
            visited += it
            it < 3
        }
        assertEquals(listOf(1, 2, 3, 4), visited)
    }
}
