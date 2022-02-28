package com.lehaine.littlekt.util.packer

import kotlin.math.floor
import kotlin.random.Random
import kotlin.test.*

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
        val position = bin.add(BinRect(0, 0, 200, 100))
        assertNotNull(position)
        expect(0) { position.x }
        expect(0) { position.y }
    }

    @Test
    fun test_set_bin_dirty_status() {
        bin.add(BinRect(0, 0, 200, 100))
        expect(true) { bin.dirty }
        bin.dirty = false
        expect(false) { bin.dirty }
        bin.add(BinRect(0, 0, 200, 100))
        expect(true) { bin.dirty }
        bin.dirty = false
        bin.dirty = true
        expect(true) { bin.dirty }
        bin.reset()
        expect(false) { bin.dirty }
        val rect = bin.add(BinRect(0, 0, 200, 100))
        assertNotNull(rect)
        bin.dirty = false
        rect.width = 256
        expect(true) { bin.dirty }
    }

    @Test
    fun test_updates_size_correctly() {
        bin.add(BinRect(0, 0, 200, 100))
        expect(256) { bin.width }
        expect(128) { bin.height }
    }

    @Test
    fun test_stores_data_correctly() {
        bin.add(BinRect(0, 0, 200, 100, data = mapOf("foo" to "bar")))
        expect("bar") { bin.rects[0].data["foo"] }
    }

    @Test
    fun test_stores_tag_and_non_tagged_rects_in_same_bin() {
        bin.add(BinRect(200, 100, data = mapOf("id" to 1), tag = "test"))
        bin.add(BinRect(200, 100, data = mapOf("id" to 2)))
        expect(2) { bin.rects.size }
        expect(1) { bin.rects[0].data["id"] }
        expect("test") { bin.rects[0].tag }
        expect(2) { bin.rects[1].data["id"] }
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

        bin.add(BinRect(width = 512, height = 1024))
        bin.add(BinRect(width = 1024, height = 512))
        expect(2) { bin.rects.size }
        expect(true) { bin.rects[1].isRotated }
        bin.reset(true)
        bin.add(BinRect(width = 512, height = 1024))
        bin.add(BinRect(width = 1024, height = 512, isRotated = true))
        expect(2) { bin.rects.size }
        expect(false) { bin.rects[1].isRotated }
    }

    @Test
    fun test_fits_squares_correctly() {
        var i = 0
        while (bin.add(BinRect(width = 100, height = 100, data = mapOf("number" to i))) != null) {
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
        bin.add(BinRect(width = 200, height = 100))
        bin.add(BinRect(width = 200, height = 100))
        bin.add(BinRect(width = 200, height = 100))
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

    @Test
    fun test_repack() {
        val rect1 = bin.add(BinRect(width = 512, height = 512, data = mapOf("id" to "one")))
        assertNotNull(rect1)
        val rect2 = bin.add(BinRect(width = 512, height = 512, data = mapOf("id" to "two")))
        assertNotNull(rect2)
        val rect3 = bin.add(BinRect(width = 512, height = 512, data = mapOf("id" to "three")))
        assertNotNull(rect3)
        rect2.width = 1014
        rect2.height = 513
        val unpacked = bin.repack()
        expect(2) { unpacked.size }
        expect("one") { unpacked[0].data["id"] }
        expect("three") { unpacked[1].data["id"] }
        expect(1) { bin.rects.size }
    }

    @Test
    fun test_monkey_testing() {
        val rects = mutableListOf<BinRect>()
        while (true) {
            val width = floor(Random.nextFloat() * 200).toInt()
            val height = floor(Random.nextFloat() * 200).toInt()
            val rect = BinRect(width = width, height = height)

            val pos = bin.add(rect)
            if (pos != null) {
                expect(width) { pos.width }
                expect(height) { pos.height }
                rects += pos
            } else {
                break
            }
        }

        expect(true) { bin.width <= 1024 }
        expect(true) { bin.height <= 1024 }

        rects.forEach { rect1 ->
            rects.forEach { rect2 ->
                if (rect1 != rect2) {
                    expect(false) { rect1.collides(rect2) }
                }
            }

            expect(true) { rect1.x >= 0 }
            expect(true) { rect1.y >= 0 }
            expect(true) { rect1.x + rect1.width <= bin.width }
            expect(true) { rect1.y + rect1.height <= bin.height }
        }
    }
}

class MaxRectsWithPaddingTests {

    private val padding = 4
    private var bin: MaxRectsBin = MaxRectsBin()
    private val options = PackingOptions().apply {
        maxWidth = 1024
        maxHeight = 1024
        allowRotation = false
        edgeBorder = 0
        paddingHorizontal = padding
        paddingVertical = padding
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
    fun test_handles_padding_correctly() {
        bin.add(BinRect(width = 512, height = 512))
        bin.add(BinRect(width = 512 - padding, height = 512))
        bin.add(BinRect(width = 512, height = 512 - padding))
        expect(1024) { bin.width }
        expect(1024) { bin.height }
        expect(3) { bin.rects.size }
    }

    @Test
    fun test_rect_with_size_close_to_the_max() {
        assertNotNull(bin.add(1024, 1024))
        expect(1) { bin.rects.size }
    }


    @Test
    fun test_monkey_testing() {
        val rects = mutableListOf<BinRect>()
        while (true) {
            val width = floor(Random.nextFloat() * 200).toInt()
            val height = floor(Random.nextFloat() * 200).toInt()
            val rect = BinRect(width = width, height = height)

            val pos = bin.add(rect)
            if (pos != null) {
                expect(width) { pos.width }
                expect(height) { pos.height }
                rects += pos
            } else {
                break
            }
        }

        expect(true) { bin.width <= 1024 }
        expect(true) { bin.height <= 1024 }

        rects.forEach { rect1 ->
            rects.forEach { rect2 ->
                if (rect1 != rect2) {
                    expect(false) { rect1.collides(rect2) }
                }
            }

            expect(true) { rect1.x >= 0 }
            expect(true) { rect1.y >= 0 }
            expect(true) { rect1.x + rect1.width <= bin.width }
            expect(true) { rect1.y + rect1.height <= bin.height }
        }
    }
}

class MaxRectsTestWithBorderAndPadding {
    private val padding = 4
    private val border = 5
    private var bin: MaxRectsBin = MaxRectsBin()
    private val options = PackingOptions().apply {
        maxWidth = 1024
        maxHeight = 1024
        allowRotation = false
        edgeBorder = border
        paddingHorizontal = padding
        paddingVertical = padding
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
    fun test_handles_border_and_padding_correctly() {
        val size = 512 - border * 2
        val pos1 = bin.add(size + 1, size)
        assertNotNull(pos1)
        expect(5) { pos1.x }
        expect(5) { pos1.y }
        expect(1024) { bin.width }
        expect(512) { bin.height }
        val pos2 = bin.add(size, size)
        assertNotNull(pos2)
        expect(padding) { pos2.x - pos1.x - pos1.width }
        expect(border) { pos2.y }
        expect(1024) { bin.width }
        expect(512) { bin.height }
        bin.add(size, size)
        bin.add(512, 508)
        expect(1024) { bin.width }
        expect(1024) { bin.height }
        expect(3) { bin.rects.size }
    }

    @Test
    fun test_rect_with_size_close_to_max() {
        assertNull(bin.add(1024, 1024))
        expect(0) { bin.rects.size }
    }

    @Test
    fun test_monkey_testing() {
        repeat(5) {
            val padding = floor(Random.nextFloat() * 10).toInt()
            val border = floor(Random.nextFloat() * 20).toInt()
            bin = MaxRectsBin(PackingOptions().apply {
                maxWidth = 1024
                maxHeight = 1024
                allowRotation = false
                edgeBorder = border
                paddingHorizontal = padding
                paddingVertical = padding
            })
            val rects = mutableListOf<BinRect>()
            while (true) {
                val width = floor(Random.nextFloat() * 200).toInt()
                val height = floor(Random.nextFloat() * 200).toInt()
                val rect = BinRect(width = width, height = height)

                val pos = bin.add(rect)
                if (pos != null) {
                    expect(width) { pos.width }
                    expect(height) { pos.height }
                    rects += pos
                } else {
                    break
                }
            }

            expect(true) { bin.width <= 1024 }
            expect(true) { bin.height <= 1024 }

            rects.forEach { rect1 ->
                rects.forEach { rect2 ->
                    if (rect1 != rect2) {
                        expect(false) { rect1.collides(rect2) }
                    }
                }

                expect(true) { rect1.x >= border }
                expect(true) { rect1.y >= border }
                expect(true) { rect1.x + rect1.width <= bin.width - border }
                expect(true) { rect1.y + rect1.height <= bin.height - border }
            }
        }
    }
}
