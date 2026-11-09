package com.littlekt.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilsTests {

    @Test
    fun test_double_to_string_precision() {
        assertEquals("1.23", 1.23456.toString(2))
        assertEquals("2.000", 2.0.toString(3))
        assertEquals("-0.1", (-0.1).toString(1))
    }

    @Test
    fun test_double_to_string_zero_precision_rounds() {
        assertEquals("3", 2.7.toString(0))
    }

    @Test
    fun test_float_to_string_precision() {
        assertEquals("0.25", 0.25f.toString(2))
    }

    @Test
    fun test_truncate_shrinks() {
        val list = mutableListOf(1, 2, 3, 4)
        list.truncate(2)
        assertEquals(listOf(1, 2), list)
    }

    @Test
    fun test_truncate_larger_size_is_noop() {
        val list = mutableListOf(1, 2)
        list.truncate(5)
        assertEquals(listOf(1, 2), list)
    }

    @Test
    fun test_truncate_to_zero_clears() {
        val list = mutableListOf(1, 2)
        list.truncate(0)
        assertEquals(emptyList(), list)
    }

    @Test
    fun test_truncate_negative_throws() {
        assertFailsWith<IllegalStateException> { mutableListOf(1).truncate(-1) }
    }
}
