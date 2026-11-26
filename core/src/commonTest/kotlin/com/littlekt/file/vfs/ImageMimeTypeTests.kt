package com.littlekt.file.vfs

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageMimeTypeTests {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun test_image_signatures_are_detected() {
        assertEquals("image/png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).imageMimeType())
        assertEquals("image/jpeg", bytes(0xFF, 0xD8, 0xFF, 0xE0).imageMimeType())
        assertEquals("image/gif", bytes(0x47, 0x49, 0x46, 0x38, 0x39, 0x61).imageMimeType())
        assertEquals("image/bmp", bytes(0x42, 0x4D, 0, 0).imageMimeType())
        assertEquals(
            "image/webp",
            bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50).imageMimeType(),
        )
    }

    @Test
    fun test_unknown_or_short_bytes_default_to_png() {
        assertEquals("image/png", bytes().imageMimeType())
        assertEquals("image/png", bytes(0xFF).imageMimeType())
        assertEquals("image/png", bytes(0x52, 0x49, 0x46, 0x46, 0, 0).imageMimeType())
    }
}
