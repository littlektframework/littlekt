package com.lehaine.littlekt.tools.texturepacker

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class MaxRectsPackerTests {

    private val options = PackingOptions().apply {
        outputPagesAsPowerOfTwo = false
        allowRotation = false
        maxWidth = 1024
        maxHeight = 1024
    }
    private var packer = MaxRectsPacker(options)

    @BeforeTest
    fun setup() {
        packer = MaxRectsPacker(options)
    }

    @Test
    fun test_adds_first_element_correctly() {
        packer.add(1000, 1000, mapOf("number" to 1))
        expect(1) { packer.bins[0].rects[0].data["number"] }
    }

    @Test
    fun test_creates_additional_bin_if_element_does_not_fit_in_existing_bin() {
        packer.add(1000, 1000, mapOf("number" to 1))
        packer.add(1000, 1000, mapOf("number" to 2))
        expect(2) { packer.bins.size }
        expect(2) { packer.bins[1].rects[0].data["number"] }
    }

    @Test
    fun test_adds_to_existing_bins_if_possible() {
        packer.add(1000, 1000, mapOf("number" to 1))
        packer.add(1000, 1000, mapOf("number" to 2))
        packer.add(10, 10, mapOf("number" to 3))
        packer.add(10, 10, mapOf("number" to 4))
        expect(2) { packer.bins.size }
    }

    @Test
    fun test_adds_to_new_bins_after_next_is_called() {
        packer.add(1000, 1000, mapOf("number" to 1))
        packer.add(1000, 1000, mapOf("number" to 2))
        packer.next()
        packer.add(10, 10, mapOf("number" to 3))
        packer.add(10, 10, mapOf("number" to 4))
        expect(3) { packer.bins.size }
        expect(2) { packer.bins.last().rects.size }
    }

    @Test
    fun test_allows_oversized_elements_to_be_added() {
        packer.add(1000, 1000, mapOf("number" to 1))
        packer.add(2000, 2000, mapOf("number" to 2))
        expect(2) { packer.bins.size }
        expect(2000) { packer.bins[1].rects[0].width }
        expect(true) { packer.bins[1].rects[0].oversized }
    }

    @Test
    fun test_sort_does_not_mutate_input_list() {
        val input = listOf(Rect(width = 1, height = 1), Rect(width = 2, height = 2))
        packer.sort(input)
        expect(1) { input[0].width }
    }

    @Test
    fun test_sort_works_correctly() {
        val input = listOf(Rect(width = 1, height = 1), Rect(width = 3, height = 1), Rect(width = 2, height = 2))
        val output = packer.sort(input)
        expect(3) { output[0].width }
        expect(2) { output[1].width }
        expect(1) { output[2].width }
    }

    @Test
    fun test_adds_multiple_elements_to_bins() {
        val input = listOf(
            Rect(width = 1000, height = 1000, data = mapOf("number" to 1)),
            Rect(width = 1000, height = 1000, data = mapOf("number" to 2))
        )
        packer.add(input)
        expect(2) { packer.bins.size }
    }

    @Test
    fun test_adds_big_rects_first() {
        val input = listOf(
            Rect(width = 600, height = 20, data = mapOf("number" to 1)),
            Rect(width = 600, height = 20, data = mapOf("number" to 2)),
            Rect(width = 1000, height = 1000, data = mapOf("number" to 3)),
            Rect(width = 1000, height = 1000, data = mapOf("number" to 4))
        )
        packer.add(input)
        expect(2) { packer.bins.size }
        expect(3) { packer.bins[0].rects[0].data["number"] }
        expect(1) { packer.bins[0].rects[1].data["number"] }
    }

    @Test
    fun test_add_empty_list() {
        packer.add(emptyList())
        expect(0) { packer.bins.size }
    }

    @Test
    fun test_add_single_element_list() {
        val input = listOf(
            Rect(width = 1000, height = 1000, data = mapOf("number" to 1)),
        )
        packer.add(input)
        expect(1) { packer.bins.size }
    }
}