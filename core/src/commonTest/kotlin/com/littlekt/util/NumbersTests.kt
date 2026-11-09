package com.littlekt.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumbersTests {

    @Test
    fun test_int_align_rounds_up() {
        assertEquals(16, 13.align(16))
        assertEquals(256, 1.align(256))
    }

    @Test
    fun test_int_align_keeps_aligned_value() {
        assertEquals(0, 0.align(4))
        assertEquals(32, 32.align(16))
    }

    @Test
    fun test_int_align_by_one() {
        assertEquals(7, 7.align(1))
    }

    @Test
    fun test_long_align() {
        assertEquals(16L, 13L.align(16L))
        assertEquals(32L, 32L.align(16L))
        assertEquals(0L, 0L.align(8L))
    }
}
