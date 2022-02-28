package com.lehaine.littlekt.util.packer

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class MaxRectsPackerTests {

    private val options = PackingOptions().apply {
        outputPagesAsPowerOfTwo = false
        allowRotation = false
        maxWidth = 1024
        maxHeight = 1024
        paddingHorizontal = 0
        paddingVertical = 0
        edgeBorder = 0
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
        val input = listOf(BinRect(width = 1, height = 1), BinRect(width = 2, height = 2))
        packer.sort(input)
        expect(1) { input[0].width }
    }

    @Test
    fun test_sort_works_correctly() {
        val input =
            listOf(BinRect(width = 1, height = 1), BinRect(width = 3, height = 1), BinRect(width = 2, height = 2))
        val output = packer.sort(input)
        expect(3) { output[0].width }
        expect(2) { output[1].width }
        expect(1) { output[2].width }
    }

    @Test
    fun test_adds_multiple_elements_to_bins() {
        val input = listOf(
            BinRect(width = 1000, height = 1000, data = mapOf("number" to 1)),
            BinRect(width = 1000, height = 1000, data = mapOf("number" to 2))
        )
        packer.add(input)
        expect(2) { packer.bins.size }
    }

    @Test
    fun test_adds_big_rects_first() {
        val input = listOf(
            BinRect(width = 600, height = 20, data = mapOf("number" to 1)),
            BinRect(width = 600, height = 20, data = mapOf("number" to 2)),
            BinRect(width = 1000, height = 1000, data = mapOf("number" to 3)),
            BinRect(width = 1000, height = 1000, data = mapOf("number" to 4))
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
            BinRect(width = 1000, height = 1000, data = mapOf("number" to 1)),
        )
        packer.add(input)
        expect(1) { packer.bins.size }
    }

    @Test
    fun test_load_old_bins_and_continue_packing() {
        val input = listOf(
            BinRect(width = 512, height = 512, data = mapOf("number" to 1)),
            BinRect(width = 512, height = 512, data = mapOf("number" to 2)),
            BinRect(width = 512, height = 512, data = mapOf("number" to 3)),
            BinRect(width = 512, height = 512, data = mapOf("number" to 4))
        )
        packer.add(input)
        expect(1) { packer.bins.size }
        expect(4) { packer.bins[0].rects.size }
        val bins = packer.save()
        expect(4) { bins[0].rects.size }
        packer.load(bins)
        expect(1) { packer.bins.size }
        expect(4) { packer.bins[0].rects.size }
        packer.add(input)
        expect(2) { packer.bins.size }
        expect(4) { packer.bins[0].rects.size }
        expect(4) { packer.bins[1].rects.size }
    }

    @Test
    fun test_quick_repack_and_deep_repack() {
        val rect = BinRect(width = 1024, height = 1024)
        packer.add(rect)
        packer.add(512, 512)
        packer.add(512, 512)
        packer.add(512, 512)
        packer.add(512, 512)
        packer.add(512, 512)
        expect(3) { packer.bins.size }
        packer.bins.forEach { it.dirty = false }
        rect.width = 512
        packer.repack()
        expect(3) { packer.bins.size }
        packer.repack(false)
        expect(2) { packer.bins.size }
        rect.width = 1024
        packer.repack()
        expect(3) { packer.bins.size }
    }

    @Test
    fun test_allow_rotation() {
        packer = MaxRectsPacker(options.clone().apply {
            allowRotation = true
        })
        packer.add(1024, 512)
        val rect = packer.add(512, 1024)
        expect(true) { rect.isRotated }
    }
}