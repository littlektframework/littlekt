package com.littlekt.graphics.g2d.font

import kotlin.test.Test
import kotlin.test.assertEquals

class TtfFontTests {

    @Test
    fun test_raster_sizes_snap_to_the_step() {
        val font = TtfFont()
        assertEquals(17f, font.snapSize(16.6f))
        assertEquals(16f, font.snapSize(16.4f))
        assertEquals(1f, font.snapSize(0.2f))
        font.rasterSizeStep = 2f
        assertEquals(18f, font.snapSize(17.2f))
        font.rasterSizeStep = 0f
        assertEquals(16.6f, font.snapSize(16.6f))
    }
}
