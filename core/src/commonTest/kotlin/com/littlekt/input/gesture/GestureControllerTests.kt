package com.littlekt.input.gesture

import com.littlekt.async.KtScope
import com.littlekt.async.MainDispatcher
import com.littlekt.input.FakeInput
import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class GestureControllerTests {

    private val input = FakeInput()
    private val events = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        KtScope.initiate()
    }

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-2f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun controller(
        tapCountInterval: Duration = 400.milliseconds,
        longPressDuration: Duration = 1100.milliseconds,
        maxFlingDuration: Duration = Duration.INFINITE,
        longPressHandled: Boolean = false,
    ) =
        GestureController(
                input,
                tapCountInterval = tapCountInterval,
                longPressDuration = longPressDuration,
                maxFlingDuration = maxFlingDuration,
            ) {
                onTouchDown { x, y, p -> events += "touchDown $x $y $p" }
                onTap { x, y, count, p -> events += "tap $x $y $count $p" }
                onLongPressHandle { x, y ->
                    events += "longPress $x $y"
                    longPressHandled
                }
                onFling { vx, vy, p -> events += "fling $vx $vy $p" }
                onPan { x, y, dx, dy -> events += "pan $x $y $dx $dy" }
                onPanStop { x, y, p -> events += "panStop $x $y $p" }
                onZoom { initial, distance -> events += "zoom $initial $distance" }
                onPinch { i1, i2, p1, p2 -> events += "pinch $i1 $i2 $p1 $p2" }
                onPinchStop { events += "pinchStop" }
            }
            .also { input.addInputProcessor(it) }

    private fun down(x: Float, y: Float, time: Long = 0L, pointer: Pointer = Pointer.POINTER1) {
        input.eventTime = time
        input.cache.onTouchDown(x, y, pointer)
        input.drain()
        MainDispatcher.INSTANCE.executePending(1.seconds)
    }

    private fun drag(x: Float, y: Float, time: Long = 0L, pointer: Pointer = Pointer.POINTER1) {
        input.eventTime = time
        input.cache.onMove(x, y, pointer)
        input.drain()
    }

    private fun up(x: Float, y: Float, time: Long = 0L, pointer: Pointer = Pointer.POINTER1) {
        input.eventTime = time
        input.cache.onTouchUp(x, y, pointer)
        input.drain()
    }

    private fun named(prefix: String) = events.filter { it.startsWith(prefix) }

    @Test
    fun test_tap_fires_on_release_inside_tap_rectangle() {
        controller()
        down(10f, 10f)
        up(12f, 12f)
        assertEquals(listOf("touchDown 10.0 10.0 POINTER1", "tap 12.0 12.0 1 POINTER1"), events)
    }

    @Test
    fun test_consecutive_taps_increase_count() {
        controller()
        down(10f, 10f)
        up(10f, 10f)
        down(11f, 10f)
        up(11f, 10f)
        assertEquals(listOf("tap 10.0 10.0 1 POINTER1", "tap 11.0 10.0 2 POINTER1"), named("tap"))
    }

    @Test
    fun test_tap_count_resets_after_interval() {
        controller(tapCountInterval = (-1).milliseconds)
        down(10f, 10f)
        up(10f, 10f)
        down(10f, 10f)
        up(10f, 10f)
        assertEquals(listOf("tap 10.0 10.0 1 POINTER1", "tap 10.0 10.0 1 POINTER1"), named("tap"))
    }

    @Test
    fun test_tap_count_resets_for_distant_tap() {
        controller()
        down(10f, 10f)
        up(10f, 10f)
        down(100f, 100f)
        up(100f, 100f)
        assertEquals(listOf("tap 10.0 10.0 1 POINTER1", "tap 100.0 100.0 1 POINTER1"), named("tap"))
    }

    @Test
    fun test_tap_count_resets_for_different_pointer() {
        controller()
        down(10f, 10f)
        up(10f, 10f)
        down(10f, 10f, pointer = Pointer.POINTER2)
        up(10f, 10f, pointer = Pointer.POINTER2)
        assertEquals(listOf("tap 10.0 10.0 1 POINTER1", "tap 10.0 10.0 1 POINTER2"), named("tap"))
    }

    @Test
    fun test_small_drag_inside_tap_rectangle_still_taps() {
        controller()
        down(10f, 10f, 0)
        drag(15f, 10f, 16)
        up(15f, 10f, 32)
        assertTrue(named("pan").isEmpty())
        assertEquals(listOf("tap 15.0 10.0 1 POINTER1"), named("tap"))
    }

    @Test
    fun test_drag_outside_tap_rectangle_pans_instead_of_tapping() {
        controller()
        down(0f, 0f, 0)
        drag(30f, 0f, 16)
        drag(40f, 5f, 32)
        up(40f, 5f, 48)
        assertEquals(
            listOf("pan 30.0 0.0 30.0 0.0", "pan 40.0 5.0 10.0 5.0", "panStop 40.0 5.0 POINTER1"),
            named("pan")
        )
        assertTrue(named("tap").isEmpty())
    }

    @Test
    fun test_fling_reports_velocity_in_pixels_per_second() {
        controller()
        down(0f, 0f, 1000)
        drag(10f, 0f, 1010)
        drag(20f, 0f, 1020)
        up(30f, 0f, 1030)
        assertEquals(listOf("fling 1000.0 0.0 POINTER1"), named("fling"))
    }

    @Test
    fun test_fling_velocity_uses_recent_samples() {
        controller()
        down(0f, 0f, 0)
        for (i in 1..25) drag(10f * i, 0f, 16L * i)
        up(260f, 0f, 416)
        val fling = named("fling").single().split(" ")
        assertNear(625f, fling[1].toFloat())
    }

    @Test
    fun test_fling_within_max_fling_duration() {
        controller(maxFlingDuration = 1.seconds)
        down(0f, 0f, 0)
        drag(30f, 0f, 16)
        up(60f, 0f, 32)
        assertEquals(1, named("fling").size)
    }

    @Test
    fun test_no_fling_past_max_fling_duration() {
        controller(maxFlingDuration = 10.milliseconds)
        down(0f, 0f, 0)
        drag(30f, 0f, 16)
        up(60f, 0f, 32)
        assertTrue(named("fling").isEmpty())
        assertEquals(1, named("panStop").size)
    }

    @Test
    fun test_two_pointers_pinch_and_zoom() {
        controller()
        down(0f, 0f, 0, Pointer.POINTER1)
        down(100f, 0f, 0, Pointer.POINTER2)
        drag(200f, 0f, 16, Pointer.POINTER2)
        up(200f, 0f, 32, Pointer.POINTER2)
        assertEquals(listOf("zoom 100.0 200.0"), named("zoom"))
        assertEquals(1, named("pinch ").size)
        assertTrue(named("pinch ").single().contains("(200.0, 0.0)"))
        assertEquals(listOf("pinchStop"), named("pinchStop"))
        assertTrue(named("tap").isEmpty())
    }

    @Test
    fun test_remaining_pointer_pans_after_pinch() {
        controller()
        down(0f, 0f, 0, Pointer.POINTER1)
        down(100f, 0f, 0, Pointer.POINTER2)
        up(100f, 0f, 16, Pointer.POINTER2)
        drag(5f, 0f, 32, Pointer.POINTER1)
        up(5f, 0f, 48, Pointer.POINTER1)
        assertEquals(listOf("pan 5.0 0.0 5.0 0.0", "panStop 5.0 0.0 POINTER1"), named("pan"))
        assertTrue(named("tap").isEmpty())
    }

    @Test
    fun test_third_pointer_is_ignored() {
        controller()
        down(0f, 0f, 0, Pointer.POINTER1)
        down(50f, 0f, 0, Pointer.POINTER2)
        down(90f, 0f, 0, Pointer.POINTER3)
        drag(95f, 0f, 16, Pointer.POINTER3)
        assertEquals(2, named("touchDown").size)
        assertTrue(named("zoom").isEmpty())
    }

    @Test
    fun test_long_press_fires_after_duration() {
        controller(longPressDuration = Duration.ZERO)
        down(3f, 4f)
        assertEquals(listOf("longPress 3.0 4.0"), named("longPress"))
    }

    @Test
    fun test_handled_long_press_suppresses_tap_and_pan() {
        controller(longPressDuration = Duration.ZERO, longPressHandled = true)
        down(3f, 4f)
        drag(50f, 4f, 16)
        up(50f, 4f, 32)
        assertTrue(named("tap").isEmpty())
        assertTrue(named("pan").isEmpty())
    }

    @Test
    fun test_long_press_not_fired_when_released_early() {
        controller()
        down(3f, 4f)
        up(3f, 4f)
        MainDispatcher.INSTANCE.executePending(1.seconds)
        assertTrue(named("longPress").isEmpty())
        assertEquals(1, named("tap").size)
    }

    @Test
    fun test_cancel_suppresses_tap() {
        val controller = controller()
        down(3f, 4f)
        controller.cancel()
        up(3f, 4f)
        assertTrue(named("tap").isEmpty())
    }

    @Test
    fun test_gesture_controller_extension_registers_processor() {
        val controller = input.gestureController {}
        assertEquals(listOf<InputProcessor>(controller), input.inputProcessors)
    }
}
