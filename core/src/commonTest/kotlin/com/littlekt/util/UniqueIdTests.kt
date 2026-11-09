package com.littlekt.util

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueIdTests {

    private class First

    private class Second

    private class Third

    @Test
    fun test_next_increments_per_type() {
        assertEquals(1, UniqueId.next<First>())
        assertEquals(2, UniqueId.next<First>())
        assertEquals(3, UniqueId.next(First::class))
    }

    @Test
    fun test_types_have_independent_counters() {
        val a = UniqueId.next<Second>()
        val b = UniqueId.next<Third>()
        assertEquals(1, a)
        assertEquals(1, b)
        assertEquals(2, UniqueId.next<Second>())
    }
}
