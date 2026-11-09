package com.littlekt.graph.node.ui

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RangeTests {

    private class TestRange : Range()

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    @Test
    fun test_defaults() {
        val range = TestRange()
        assertEquals(0f, range.min)
        assertEquals(100f, range.max)
        assertEquals(1f, range.step)
        assertEquals(0f, range.value)
    }

    @Test
    fun test_value_is_clamped() {
        val range = TestRange()
        range.value = 150f
        assertEquals(100f, range.value)
        range.value = -5f
        assertEquals(0f, range.value)
    }

    @Test
    fun test_allow_greater_and_lesser() {
        val range = TestRange().apply {
            allowGreater = true
            allowLesser = true
        }
        range.value = 150f
        assertEquals(150f, range.value)
        range.value = -5f
        assertEquals(-5f, range.value)
    }

    @Test
    fun test_value_snaps_to_step() {
        val range = TestRange()
        range.value = 2.4f
        assertEquals(2f, range.value)
        range.step = 0.5f
        range.value = 2.3f
        assertEquals(2.5f, range.value)
        range.step = 0f
        range.value = 2.3f
        assertEquals(2.3f, range.value)
    }

    @Test
    fun test_rounded_rounds_to_integer() {
        val range = TestRange().apply {
            step = 0f
            rounded = true
        }
        range.value = 2.6f
        assertEquals(3f, range.value)
    }

    @Test
    fun test_page_limits_value() {
        val range = TestRange().apply { page = 20f }
        range.value = 100f
        assertEquals(80f, range.value)
    }

    @Test
    fun test_page_is_clamped_to_range() {
        val range = TestRange()
        range.page = 500f
        assertEquals(100f, range.page)
        range.page = -5f
        assertEquals(0f, range.page)
    }

    @Test
    fun test_min_above_max_pushes_max() {
        val range = TestRange()
        range.min = 200f
        assertEquals(200f, range.max)
        assertEquals(200f, range.value)
    }

    @Test
    fun test_lowering_max_clamps_value() {
        val range = TestRange()
        range.value = 80f
        range.max = 50f
        assertEquals(50f, range.value)
    }

    @Test
    fun test_value_changed_emits_only_on_change() {
        val range = TestRange()
        val values = mutableListOf<Float>()
        range.onValueChanged += { values += it }
        range.value = 10f
        range.value = 10f
        range.value = 10.2f
        range.value = 200f
        assertEquals(listOf(10f, 100f), values)
    }

    @Test
    fun test_ratio_maps_value_between_min_and_max() {
        val range = TestRange()
        range.value = 25f
        assertNear(0.25f, range.ratio)
    }

    @Test
    fun test_ratio_with_offset_min() {
        val range = TestRange().apply {
            min = 10f
            max = 110f
        }
        range.value = 35f
        assertNear(0.25f, range.ratio)
    }

    @Test
    fun test_ratio_is_one_when_range_is_empty() {
        val range = TestRange().apply { max = 0f }
        assertEquals(1f, range.ratio)
    }

    @Test
    fun test_set_ratio_maps_to_value() {
        val range = TestRange().apply {
            min = 10f
            max = 110f
        }
        range.ratio = 0.5f
        assertEquals(60f, range.value)
        range.ratio = 2f
        assertEquals(110f, range.value)
    }
}
