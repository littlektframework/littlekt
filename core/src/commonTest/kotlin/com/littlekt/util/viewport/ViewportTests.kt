package com.littlekt.util.viewport

import com.littlekt.util.Scaler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewportTests {

    private fun assertBounds(x: Int, y: Int, width: Int, height: Int, viewport: Viewport) {
        assertEquals(x, viewport.x, "x")
        assertEquals(y, viewport.y, "y")
        assertEquals(width, viewport.width, "width")
        assertEquals(height, viewport.height, "height")
    }

    private fun assertVirtual(width: Float, height: Float, viewport: Viewport) {
        assertEquals(width, viewport.virtualWidth, "virtualWidth")
        assertEquals(height, viewport.virtualHeight, "virtualHeight")
        assertEquals(width, viewport.camera.virtualWidth, "camera.virtualWidth")
        assertEquals(height, viewport.camera.virtualHeight, "camera.virtualHeight")
    }

    @Test
    fun test_base_viewport_defaults_virtual_to_size() {
        val viewport = Viewport(0, 0, 320, 240)
        assertEquals(320f, viewport.virtualWidth)
        assertEquals(240f, viewport.virtualHeight)
        assertEquals(320f / 240f, viewport.aspectRatio)
    }

    @Test
    fun test_base_viewport_update_only_applies() {
        val viewport = Viewport(0, 0, 320, 240)
        viewport.update(800, 600)
        assertBounds(0, 0, 320, 240, viewport)
        assertVirtual(320f, 240f, viewport)
    }

    @Test
    fun test_is_in_viewport() {
        val viewport = Viewport(10, 20, 100, 50)
        assertTrue(viewport.isInViewport(10f, 20f))
        assertTrue(viewport.isInViewport(109.9f, 69.9f))
        assertFalse(viewport.isInViewport(110f, 30f))
        assertFalse(viewport.isInViewport(50f, 70f))
        assertFalse(viewport.isInViewport(9f, 30f))
    }

    @Test
    fun test_apply_centers_camera() {
        val viewport = Viewport(0, 0, 200, 100)
        viewport.apply(centerCamera = true)
        assertEquals(100f, viewport.camera.position.x)
        assertEquals(50f, viewport.camera.position.y)
    }

    @Test
    fun test_apply_without_center_keeps_camera_position() {
        val viewport = Viewport(0, 0, 200, 100)
        viewport.camera.position.set(5f, 6f, 0f)
        viewport.apply()
        assertEquals(5f, viewport.camera.position.x)
        assertEquals(6f, viewport.camera.position.y)
    }

    @Test
    fun test_screen_viewport_matches_window() {
        val viewport = ScreenViewport(0, 0)
        viewport.update(300, 200)
        assertBounds(0, 0, 300, 200, viewport)
        assertVirtual(300f, 200f, viewport)
    }

    @Test
    fun test_screen_viewport_units_per_pixel() {
        val viewport = ScreenViewport(0, 0)
        viewport.unitsPerPixel = 2f
        viewport.update(300, 200, centerCamera = true)
        assertVirtual(600f, 400f, viewport)
        assertEquals(300f, viewport.camera.position.x)
        assertEquals(200f, viewport.camera.position.y)
    }

    @Test
    fun test_stretch_viewport_fills_window_keeps_virtual() {
        val viewport = StretchViewport(100, 50)
        viewport.update(300, 200)
        assertBounds(0, 0, 300, 200, viewport)
        assertVirtual(100f, 50f, viewport)
    }

    @Test
    fun test_fit_viewport_letterboxes() {
        val viewport = ScalingViewport(Scaler.Fit(), 100, 50)
        viewport.update(400, 400)
        assertBounds(0, 100, 400, 200, viewport)
        assertVirtual(100f, 50f, viewport)
    }

    @Test
    fun test_fit_viewport_pillarboxes() {
        val viewport = ScalingViewport(Scaler.Fit(), 100, 100)
        viewport.update(400, 200)
        assertBounds(100, 0, 200, 200, viewport)
    }

    @Test
    fun test_fill_viewport_overflows() {
        val viewport = ScalingViewport(Scaler.Fill(), 100, 50)
        viewport.update(400, 400)
        assertBounds(-200, 0, 800, 400, viewport)
    }

    @Test
    fun test_scaling_viewport_repeated_updates() {
        val viewport = ScalingViewport(Scaler.Fit(), 100, 50)
        viewport.update(400, 400)
        viewport.update(200, 100)
        assertBounds(0, 0, 200, 100, viewport)
        assertVirtual(100f, 50f, viewport)
    }

    @Test
    fun test_extend_viewport_extends_width() {
        val viewport = ExtendViewport(100, 100)
        viewport.update(400, 200)
        assertBounds(0, 0, 400, 200, viewport)
        assertVirtual(200f, 100f, viewport)
    }

    @Test
    fun test_extend_viewport_extends_height() {
        val viewport = ExtendViewport(100, 100)
        viewport.update(100, 200)
        assertBounds(0, 0, 100, 200, viewport)
        assertVirtual(100f, 200f, viewport)
    }

    @Test
    fun test_extend_viewport_same_aspect_keeps_min_size() {
        val viewport = ExtendViewport(100, 50)
        viewport.update(300, 150)
        assertBounds(0, 0, 300, 150, viewport)
        assertVirtual(100f, 50f, viewport)
    }

    @Test
    fun test_extend_viewport_shrink_back() {
        val viewport = ExtendViewport(100, 100)
        viewport.update(400, 200)
        viewport.update(200, 200)
        assertBounds(0, 0, 200, 200, viewport)
        assertVirtual(100f, 100f, viewport)
    }
}
