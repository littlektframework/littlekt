package com.littlekt.graphics

import com.littlekt.math.Vec4f
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ColorTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertColor(r: Float, g: Float, b: Float, a: Float, actual: Color, eps: Float = 1e-4f) {
        assertTrue(actual.isFuzzyEqual(Vec4f(r, g, b, a), eps), "expected ($r, $g, $b, $a) but was $actual")
    }

    @Test
    fun test_constructor_channels() {
        val color = Color(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(0.1f, color.r)
        assertEquals(0.2f, color.g)
        assertEquals(0.3f, color.b)
        assertEquals(0.4f, color.a)
        assertEquals(color.x, color.r)
        assertEquals(color.w, color.a)
    }

    @Test
    fun test_default_alpha_is_opaque() {
        assertEquals(1f, Color(0f, 0f, 0f).a)
        assertEquals(1f, MutableColor().a)
    }

    @Test
    fun test_copy_constructors() {
        val color = Color(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(color, Color(color))
        assertEquals(color, MutableColor(color))
        assertEquals(color, color.toMutableColor())
    }

    @Test
    fun test_equality_between_color_and_mutable_color() {
        assertEquals<Color>(Color.RED, MutableColor(1f, 0f, 0f, 1f))
        assertEquals(Color.RED.hashCode(), MutableColor(1f, 0f, 0f, 1f).hashCode())
        assertNotEquals(Color.RED, Color.BLUE)
    }

    @Test
    fun test_brightness() {
        assertNear(1f, Color.WHITE.brightness)
        assertNear(0f, Color.BLACK.brightness)
        assertNear(0.299f, Color.RED.brightness)
        assertNear(0.587f, Color.GREEN.brightness)
    }

    @Test
    fun test_mix() {
        assertColor(0.5f, 0f, 0.5f, 1f, Color.RED.mix(Color.BLUE, 0.5f))
        assertColor(1f, 0f, 0f, 1f, Color.RED.mix(Color.BLUE, 0f))
        assertColor(0f, 0f, 1f, 1f, Color.RED.mix(Color.BLUE, 1f))
        assertColor(0.75f, 0.75f, 0.75f, 0.75f, Color.WHITE.mix(Color.CLEAR, 0.25f))
    }

    @Test
    fun test_mix_into_self() {
        val color = MutableColor(1f, 0f, 0f, 1f)
        color.mix(Color.BLUE, 0.25f, color)
        assertColor(0.75f, 0f, 0.25f, 1f, color)
    }

    @Test
    fun test_scale_rgb_keeps_alpha() {
        assertColor(0.25f, 0.5f, 0.125f, 0.5f, Color(0.5f, 1f, 0.25f, 0.5f).scaleRgb(0.5f))
    }

    @Test
    fun test_with_alpha() {
        assertColor(1f, 0f, 0f, 0.3f, Color.RED.withAlpha(0.3f))
    }

    @Test
    fun test_to_linear_uses_gamma_2_2() {
        val linear = Color(0.5f, 0f, 1f, 0.5f).toLinear()
        assertNear(0.5f.pow(2.2f), linear.r)
        assertNear(0.2176f, linear.r)
        assertNear(0f, linear.g)
        assertNear(1f, linear.b)
        assertNear(0.5f, linear.a)
    }

    @Test
    fun test_to_linear_is_close_to_srgb_curve() {
        assertNear(0.214f, Color(0.5f, 0.5f, 0.5f).toLinear().r, 5e-3f)
    }

    @Test
    fun test_to_srgb() {
        val srgb = Color(0.2176f, 0f, 1f, 0.5f).toSrgb()
        assertNear(0.5f, srgb.r, 1e-3f)
        assertNear(0.5f, srgb.a)
        assertNear(0.7297f, Color(0.5f, 0.5f, 0.5f).toSrgb().r, 1e-3f)
    }

    @Test
    fun test_linear_srgb_round_trip() {
        for (i in 0..20) {
            val v = i / 20f
            val color = Color(v, 1f - v, v * 0.5f, 0.7f)
            assertColor(v, 1f - v, v * 0.5f, 0.7f, color.toLinear().toSrgb(), 1e-4f)
            assertColor(v, 1f - v, v * 0.5f, 0.7f, color.toSrgb().toLinear(), 1e-4f)
        }
    }

    @Test
    fun test_gamma_into_self() {
        val color = MutableColor(0.25f, 0.5f, 1f, 0.5f)
        color.gamma(2f, color)
        assertColor(0.0625f, 0.25f, 1f, 0.5f, color)
    }

    @Test
    fun test_lighten() {
        assertColor(0.1f, 0.1f, 0.1f, 1f, Color.BLACK.lighten())
        assertColor(0.5f, 0.5f, 0.5f, 1f, Color.BLACK.lighten(0.5f))
        assertColor(1f, 1f, 1f, 1f, Color.WHITE.lighten(0.5f))
        assertColor(1f, 0.5f, 0.5f, 0.5f, MutableColor(1f, 0f, 0f, 0.5f).lighten(0.5f))
    }

    @Test
    fun test_from_hex_rgb() {
        assertColor(1f, 128 / 255f, 0f, 1f, Color.fromHex("#ff8000"))
        assertColor(1f, 128 / 255f, 0f, 1f, Color.fromHex("FF8000"))
    }

    @Test
    fun test_from_hex_rgba() {
        assertColor(0x12 / 255f, 0x34 / 255f, 0x56 / 255f, 0x78 / 255f, Color.fromHex("#12345678"))
    }

    @Test
    fun test_from_hex_short_rgb() {
        assertColor(1f, 0x88 / 255f, 0f, 1f, Color.fromHex("#f80"))
    }

    @Test
    fun test_from_hex_short_rgba() {
        assertColor(0x11 / 255f, 0x22 / 255f, 0x33 / 255f, 0x44 / 255f, Color.fromHex("#1234"))
        assertColor(1f, 0x88 / 255f, 0f, 0xcc / 255f, Color.fromHex("f80c"))
    }

    @Test
    fun test_from_hex_empty_or_invalid_length_is_black() {
        assertEquals(Color.BLACK, Color.fromHex(""))
        assertEquals(Color.BLACK, Color.fromHex("#12345"))
    }

    @Test
    fun test_to_hex_string() {
        assertEquals("ff0000ff", Color.RED.toHexString())
        assertEquals("12345678", Color.fromHex("#12345678").toHexString())
        assertEquals("00000000", Color.CLEAR.toHexString())
        assertEquals("0a0b0c0d", Color.fromHex("0a0b0c0d").toHexString())
    }

    @Test
    fun test_to_hex_string_clamps_out_of_range() {
        assertEquals("ff0000ff", Color(2f, -1f, 0f, 1f).toHexString())
    }

    @Test
    fun test_hex_round_trip() {
        for (hex in listOf("ff8000ff", "00000000", "ffffffff", "7f7f7f80", "010203fe")) {
            assertEquals(hex, Color.fromHex(hex).toHexString())
        }
    }

    @Test
    fun test_rgba8888() {
        assertEquals(0xff0000ff.toInt(), Color.RED.toRgba8888())
        assertEquals(0x00ff00ff, Color.GREEN.rgba())
        assertEquals(0xffffffff.toInt(), Color.WHITE.toRgba8888())
        assertEquals(0, Color.CLEAR.toRgba8888())
    }

    @Test
    fun test_abgr888() {
        assertEquals(0xff0000ff.toInt(), Color.RED.toAbgr888())
        assertEquals(0xffff0000.toInt(), Color.BLUE.abgr())
        assertEquals(0x80000000.toInt() or 0x00ff00, Color(0f, 1f, 0f, 128 / 255f).toAbgr888())
    }

    @Test
    fun test_set_rgba8888() {
        assertColor(0x12 / 255f, 0x34 / 255f, 0x56 / 255f, 0x78 / 255f, MutableColor().setRgba8888(0x12345678))
        assertColor(1f, 0f, 0f, 1f, MutableColor().setRgba8888(0xff0000ff.toInt()))
    }

    @Test
    fun test_set_abgr888() {
        assertColor(0x78 / 255f, 0x56 / 255f, 0x34 / 255f, 0x12 / 255f, MutableColor().setAbgr888(0x12345678))
        assertColor(0f, 0f, 1f, 1f, MutableColor().setAbgr888(0xffff0000.toInt()))
    }

    @Test
    fun test_packed_round_trips_for_every_channel_value() {
        val color = MutableColor()
        for (i in 0..255) {
            val v = i / 255f
            val rgba = Color(v, v, v, v).toRgba8888()
            assertEquals(i, rgba and 0xff, "rgba channel $i")
            assertEquals(Color(v, v, v, v), color.setRgba8888(rgba))
            val abgr = Color(v, v, v, v).toAbgr888()
            assertEquals(i, abgr and 0xff, "abgr channel $i")
            assertEquals(Color(v, v, v, v), color.setAbgr888(abgr))
        }
    }

    @Test
    fun test_to_float_bits() {
        val bits = Color.WHITE.toFloatBits().toRawBits()
        assertEquals(0xfeffffff.toInt(), bits)
        val red = Color.RED.toFloatBits().toRawBits()
        assertEquals(0xfe0000ff.toInt(), red)
    }

    @Test
    fun test_from_hsv_primaries() {
        assertColor(1f, 0f, 0f, 1f, Color.fromHsv(0f, 1f, 1f, 1f))
        assertColor(1f, 1f, 0f, 1f, Color.fromHsv(60f, 1f, 1f, 1f))
        assertColor(0f, 1f, 0f, 1f, Color.fromHsv(120f, 1f, 1f, 1f))
        assertColor(0f, 1f, 1f, 1f, Color.fromHsv(180f, 1f, 1f, 1f))
        assertColor(0f, 0f, 1f, 1f, Color.fromHsv(240f, 1f, 1f, 1f))
        assertColor(1f, 0f, 1f, 1f, Color.fromHsv(300f, 1f, 1f, 0.5f).withAlpha(1f))
    }

    @Test
    fun test_from_hsv_wraps_hue_and_handles_gray() {
        assertColor(1f, 0f, 0f, 1f, Color.fromHsv(360f, 1f, 1f, 1f))
        assertColor(0f, 0f, 1f, 1f, Color.fromHsv(-120f, 1f, 1f, 1f))
        assertColor(0.5f, 0.5f, 0.5f, 1f, Color.fromHsv(77f, 0f, 0.5f, 1f))
        assertColor(1f, 0.5f, 0f, 0.25f, Color.fromHsv(30f, 1f, 1f, 0.25f))
    }

    @Test
    fun test_mutable_add_subtract_scale_mul() {
        val color = MutableColor(0.1f, 0.2f, 0.3f, 0.4f)
        color.add(Vec4f(0.1f, 0.1f, 0.1f, 0.1f))
        assertColor(0.2f, 0.3f, 0.4f, 0.5f, color)
        color.add(Vec4f(1f, 1f, 1f, 1f), 0.5f)
        assertColor(0.7f, 0.8f, 0.9f, 1f, color)
        color.subtract(Vec4f(0.5f, 0.5f, 0.5f, 0.5f))
        assertColor(0.2f, 0.3f, 0.4f, 0.5f, color)
        color.scale(2f)
        assertColor(0.4f, 0.6f, 0.8f, 1f, color)
        color.mul(Vec4f(0.5f, 0f, 1f, 0.5f))
        assertColor(0.2f, 0f, 0.8f, 0.5f, color)
    }

    @Test
    fun test_mutable_add_self() {
        val color = MutableColor(0.1f, 0.2f, 0.3f, 0.4f)
        color.add(color)
        assertColor(0.2f, 0.4f, 0.6f, 0.8f, color)
        color.mul(color)
        assertColor(0.04f, 0.16f, 0.36f, 0.64f, color)
    }

    @Test
    fun test_mutable_set_and_clear() {
        val color = MutableColor()
        color.set(0.1f, 0.2f, 0.3f, 0.4f)
        assertColor(0.1f, 0.2f, 0.3f, 0.4f, color)
        color.set(Color.RED)
        assertEquals<Color>(Color.RED, color)
        color[1] = 0.5f
        assertEquals(0.5f, color.g)
        color.clear()
        assertEquals<Color>(Color.CLEAR, color)
    }

    @Test
    fun test_mutable_channel_setters() {
        val color = MutableColor()
        color.r = 0.1f
        color.g = 0.2f
        color.b = 0.3f
        color.a = 0.4f
        assertColor(0.1f, 0.2f, 0.3f, 0.4f, color)
    }

    @Test
    fun test_result_parameter_is_returned() {
        val result = MutableColor()
        assertTrue(Color.RED.mix(Color.BLUE, 0.5f, result) === result)
        assertTrue(Color.RED.toLinear(result) === result)
        assertTrue(Color.RED.lighten(0.1f, result) === result)
        assertTrue(Color.RED.scaleRgb(0.1f, result) === result)
        assertTrue(Color.RED.toMutableColor(result) === result)
    }
}
