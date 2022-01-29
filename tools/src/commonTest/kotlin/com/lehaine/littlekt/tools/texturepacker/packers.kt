package com.lehaine.littlekt.tools.texturepacker

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class MaxRectsPackerTest {

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
        expect(2) { packer.bins.last().rects.size}
    }

    @Test
    fun test_allows_oversized_elements_to_be_added() {
        packer.add(1000, 1000, mapOf("number" to 1))
        packer.add(2000, 2000, mapOf("number" to 2))
        expect(2) { packer.bins.size }
        expect(2000) { packer.bins[1].rects[0].width}
        expect(true) { packer.bins[1].rects[0].oversized}
    }
}