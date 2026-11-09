package com.littlekt.util

import com.littlekt.math.Vec2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ScalerTests {

    private fun assertSize(width: Float, height: Float, actual: Vec2f) {
        assertEquals(width, actual.x)
        assertEquals(height, actual.y)
    }

    @Test
    fun test_fit_letterboxes_wide_source() {
        assertSize(400f, 200f, Scaler.Fit().apply(100f, 50f, 400f, 400f))
    }

    @Test
    fun test_fit_pillarboxes_tall_source() {
        assertSize(200f, 400f, Scaler.Fit().apply(50f, 100f, 400f, 400f))
    }

    @Test
    fun test_fill_covers_target() {
        assertSize(800f, 400f, Scaler.Fill().apply(100f, 50f, 400f, 400f))
        assertSize(400f, 800f, Scaler.Fill().apply(50f, 100f, 400f, 400f))
    }

    @Test
    fun test_fill_x_and_fill_y() {
        assertSize(300f, 150f, Scaler.FillX().apply(100f, 50f, 300f, 400f))
        assertSize(800f, 400f, Scaler.FillY().apply(100f, 50f, 300f, 400f))
    }

    @Test
    fun test_stretch_variants() {
        assertSize(300f, 400f, Scaler.Stretch().apply(100f, 50f, 300f, 400f))
        assertSize(300f, 50f, Scaler.StretchX().apply(100f, 50f, 300f, 400f))
        assertSize(100f, 400f, Scaler.StretchY().apply(100f, 50f, 300f, 400f))
    }

    @Test
    fun test_none_returns_source() {
        assertSize(100f, 50f, Scaler.None().apply(100f, 50f, 300f, 400f))
    }

    @Test
    fun test_same_aspect_scales_uniformly() {
        assertSize(300f, 150f, Scaler.Fit().apply(100f, 50f, 300f, 150f))
        assertSize(300f, 150f, Scaler.Fill().apply(100f, 50f, 300f, 150f))
    }

    @Test
    fun test_int_overload() {
        assertSize(400f, 200f, Scaler.Fit().apply(100, 50, 400, 400))
    }

    @Test
    fun test_result_is_reused() {
        val scaler = Scaler.Fit()
        val a = scaler.apply(1f, 1f, 2f, 2f)
        val b = scaler.apply(1f, 1f, 4f, 4f)
        assertSame(a, b)
        assertSize(4f, 4f, b)
    }
}
