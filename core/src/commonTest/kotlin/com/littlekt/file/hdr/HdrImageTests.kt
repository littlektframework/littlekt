package com.littlekt.file.hdr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HdrImageTests {

    private fun header(width: Int, height: Int, y: String = "-Y") =
        "#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n$y $height +X $width\n".encodeToByteArray()

    @Test
    fun test_decode_flat() {
        val pixels = byteArrayOf(128.toByte(), 64, 0, 129.toByte(), 0, 0, 0, 0)
        val image = decodeRadianceHdr(header(2, 1) + pixels)
        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertEquals(1f, image.data[0])
        assertEquals(0.5f, image.data[1])
        assertEquals(0f, image.data[2])
        assertEquals(0f, image.data[3])
    }

    @Test
    fun test_decode_rle() {
        val width = 8
        val scanline = mutableListOf<Byte>(2, 2, 0, width.toByte())
        scanline += listOf((128 + width).toByte(), 128.toByte())
        scanline += listOf(width.toByte()) + List(width) { (it * 16).toByte() }
        scanline += listOf((128 + width).toByte(), 0)
        scanline += listOf((128 + width).toByte(), 129.toByte())
        val image = decodeRadianceHdr(header(width, 1) + scanline.toByteArray())
        for (x in 0 until width) {
            assertEquals(1f, image.data[x * 3])
            assertEquals(x * 16 / 128f, image.data[x * 3 + 1])
            assertEquals(0f, image.data[x * 3 + 2])
        }
    }

    @Test
    fun test_decode_flip_y() {
        val pixels = byteArrayOf(128.toByte(), 0, 0, 129.toByte(), 0, 128.toByte(), 0, 129.toByte())
        val image = decodeRadianceHdr(header(1, 2, "+Y") + pixels)
        assertEquals(0f, image.data[0])
        assertEquals(1f, image.data[1])
        assertEquals(1f, image.data[3])
    }

    @Test
    fun test_decode_rejects_other_files() {
        assertFailsWith<IllegalArgumentException> {
            decodeRadianceHdr("PNG".encodeToByteArray())
        }
    }

    @Test
    fun test_float_to_half() {
        assertEquals(0x3c00, HdrImage.floatToHalf(1f))
        assertEquals(0x3800, HdrImage.floatToHalf(0.5f))
        assertEquals(0xc000, HdrImage.floatToHalf(-2f))
        assertEquals(0x0000, HdrImage.floatToHalf(0f))
        assertEquals(0x7bff, HdrImage.floatToHalf(100000f))
        assertEquals(0x0001, HdrImage.floatToHalf(5.9604645e-8f))
    }
}
