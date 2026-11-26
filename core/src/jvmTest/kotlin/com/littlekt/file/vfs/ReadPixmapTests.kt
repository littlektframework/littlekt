package com.littlekt.file.vfs

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ReadPixmapTests {

    private fun encode(format: String, type: Int): ByteArray {
        val image = BufferedImage(3, 2, type)
        image.setRGB(0, 0, 0xFFFF0000.toInt())
        image.setRGB(1, 0, 0xFF00FF00.toInt())
        image.setRGB(2, 0, 0xFF0000FF.toInt())
        image.setRGB(0, 1, if (type == BufferedImage.TYPE_INT_ARGB) 0x80FFFFFF.toInt() else -1)
        image.setRGB(1, 1, 0xFF000000.toInt())
        image.setRGB(2, 1, -1)
        return ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()
    }

    @Test
    fun test_png_bytes_decode_to_rgba_pixels() = runTest {
        val pixmap = encode("png", BufferedImage.TYPE_INT_ARGB).readPixmap()
        assertEquals(3, pixmap.width)
        assertEquals(2, pixmap.height)
        val p = pixmap.pixels
        assertEquals(listOf(255, 0, 0, 255), (0 until 4).map { p.getByte(it).toInt() and 0xFF })
        assertEquals(listOf(0, 255, 0, 255), (4 until 8).map { p.getByte(it).toInt() and 0xFF })
        assertEquals(listOf(0, 0, 255, 255), (8 until 12).map { p.getByte(it).toInt() and 0xFF })
        assertEquals(0x80, p.getByte(15).toInt() and 0xFF)
    }

    @Test
    fun test_jpeg_bytes_decode_to_rgba_pixels() = runTest {
        val bytes = encode("jpg", BufferedImage.TYPE_INT_RGB)
        assertEquals("image/jpeg", bytes.imageMimeType())
        val pixmap = bytes.readPixmap()
        assertEquals(3, pixmap.width)
        assertEquals(2, pixmap.height)
        val alpha = pixmap.pixels.getByte(3).toInt() and 0xFF
        assertEquals(255, alpha)
        val black = (16 until 19).map { pixmap.pixels.getByte(it).toInt() and 0xFF }
        assertTrue(black.all { abs(it) < 60 })
    }

    @Test
    fun test_invalid_bytes_fail_to_decode() = runTest {
        assertFailsWith<IllegalStateException> { byteArrayOf(1, 2, 3, 4).readPixmap() }
    }
}
