package com.lehaine.littlekt.tools.texturepacker

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.expect

class MaxRectsNoPaddingTests {
    private var bin: MaxRectsBin = MaxRectsBin()
    private val options = PackingOptions().apply {
        maxWidth = 1024
        maxHeight = 1024
        allowRotation = false
        edgeBorder = 0
        paddingHorizontal = 0
        paddingVertical = 0
    }

    @BeforeTest
    fun setup() {
        bin = MaxRectsBin(options)
    }

    @Test
    fun test_is_initially_empty() {
        expect(0) { bin.width }
        expect(0) { bin.height }
    }

    @Test
    fun test_adds_rects_correctly() {
        val position = bin.add(Rect(0, 0, 200, 100))
        assertNotNull(position)
        expect(0) { position.x }
        expect(0) { position.y }
    }

    @Test
    fun test_set_bin_dirty_status() {
        bin.add(Rect(0, 0, 200, 100))
        expect(true) { bin.dirty }
        bin.dirty = false
        expect(false) { bin.dirty }
        bin.add(Rect(0, 0, 200, 100))
        expect(true) { bin.dirty }
        bin.dirty = false
        bin.dirty = true
        expect(true) { bin.dirty }
        bin.reset()
        expect(false) { bin.dirty }
        val rect = bin.add(Rect(0, 0, 200, 100))
        assertNotNull(rect)
        bin.dirty = false
        rect.width = 256
        expect(true) { bin.dirty }
    }

    @Test
    fun test_updates_size_correctly() {
        bin.add(Rect(0, 0, 200, 100))
        expect(256) { bin.width }
        expect(128) { bin.height }
    }

    @Test
    fun test_stores_data_correctly() {
        bin.add(Rect(0, 0, 200, 100, data = mapOf("foo" to "bar")))
        expect("bar") { bin.rects[0].data["foo"] }
    }

    @Test
    fun test_set_rotation_correctly() {
        bin = MaxRectsBin(PackingOptions().apply {
            maxWidth = 1024
            maxHeight = 1024
            paddingHorizontal = 0
            paddingVertical = 0
            edgeBorder = 0
            allowRotation = true
        })

        bin.add(Rect(width = 512, height = 1024))
        bin.add(Rect(width = 1024, height = 512))
        expect(2) { bin.rects.size }
        expect(true) { bin.rects[1].isRotated }
        bin.reset(true)
        bin.add(Rect(width = 512, height = 1024))
        bin.add(Rect(width = 1024, height = 512, isRotated = true))
        expect(2) { bin.rects.size }
        expect(false) { bin.rects[1].isRotated }
    }

    @Test
    fun test_fits_squares_correctly() {
        var i = 0
        while (bin.add(Rect(width = 100, height = 100, data = mapOf("number" to i))) != null) {
            if (i++ == 1000) break
        }
        expect(100) { i }
        expect(100) { bin.rects.size }
        expect(1024) { bin.width }
        expect(1024) { bin.height }

        bin.rects.forEachIndexed { index, rect -> expect(index) { rect.data["number"] } }

    }

    @Test
    fun test_reset_and_deep_reset() {
        bin.add(Rect(width = 200, height = 100))
        bin.add(Rect(width = 200, height = 100))
        bin.add(Rect(width = 200, height = 100))
        expect(3) { bin.rects.size }
        expect(512) { bin.width }
        bin.reset()
        expect(0) { bin.width }
        expect(1) { bin.freeRects.size }
        val unpacked = bin.repack()
        expect(0) { unpacked.size }
        expect(512) { bin.width }
        bin.reset(true)
        expect(0) { bin.width }
        expect(0) { bin.rects.size }
    }
}