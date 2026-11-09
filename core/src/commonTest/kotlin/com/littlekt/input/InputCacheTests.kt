package com.littlekt.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputCacheTests {

    private class Recorder : InputProcessor {
        val events = mutableListOf<String>()

        override fun keyDown(key: Key): Boolean {
            events += "down $key"
            return false
        }

        override fun keyUp(key: Key): Boolean {
            events += "up $key"
            return false
        }

        override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
            events += "drag ${screenX.str()} ${screenY.str()} $pointer"
            return false
        }

        override fun mouseMoved(screenX: Float, screenY: Float): Boolean {
            events += "move ${screenX.str()} ${screenY.str()}"
            return false
        }

        override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
            events += "pressed $button ${pressure.str()} $gamepad"
            return false
        }

        override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
            events += "released $button $gamepad"
            return false
        }

        override fun gamepadJoystickMoved(
            stick: GameStick,
            xAxis: Float,
            yAxis: Float,
            gamepad: Int
        ): Boolean {
            events += "stick $stick ${xAxis.str()} ${yAxis.str()} $gamepad"
            return false
        }

        override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
            events += "trigger $button ${pressure.str()} $gamepad"
            return false
        }
    }

    @Test
    fun test_key_down_sets_pressed_and_just_pressed() {
        val cache = InputCache()
        cache.onKeyDown(Key.A)
        assertTrue(cache.isKeyPressed(Key.A))
        assertTrue(cache.isKeyJustPressed(Key.A))
        assertTrue(cache.anyKeyPressed)
        assertTrue(cache.anyKeyJustPressed)
        assertFalse(cache.isKeyPressed(Key.B))
    }

    @Test
    fun test_reset_clears_just_pressed_but_keeps_pressed() {
        val cache = InputCache()
        cache.onKeyDown(Key.A)
        cache.reset()
        assertTrue(cache.isKeyPressed(Key.A))
        assertFalse(cache.isKeyJustPressed(Key.A))
        assertFalse(cache.anyKeyJustPressed)
    }

    @Test
    fun test_key_up_sets_just_released_until_reset() {
        val cache = InputCache()
        cache.onKeyDown(Key.A)
        cache.reset()
        cache.onKeyUp(Key.A)
        assertFalse(cache.isKeyPressed(Key.A))
        assertTrue(cache.isKeyJustReleased(Key.A))
        assertTrue(cache.anyKeyJustReleased)
        cache.reset()
        assertFalse(cache.isKeyJustReleased(Key.A))
    }

    @Test
    fun test_duplicate_key_down_counts_once() {
        val cache = InputCache()
        cache.onKeyDown(Key.A)
        cache.onKeyDown(Key.A)
        cache.onKeyUp(Key.A)
        assertFalse(cache.anyKeyPressed)
    }

    @Test
    fun test_key_up_without_down_is_ignored() {
        val cache = InputCache()
        cache.onKeyUp(Key.A)
        assertFalse(cache.isKeyJustReleased(Key.A))
        assertFalse(cache.anyKeyJustReleased)
        cache.onKeyDown(Key.B)
        assertTrue(cache.anyKeyPressed)
    }

    @Test
    fun test_key_events_queue_until_processed() {
        val cache = InputCache()
        val recorder = Recorder()
        cache.onKeyDown(Key.A)
        cache.onKeyDown(Key.A)
        cache.onKeyUp(Key.A)
        cache.processEvents(listOf(recorder))
        assertEquals(listOf("down A", "up A"), recorder.events)
        cache.processEvents(listOf(recorder))
        assertEquals(2, recorder.events.size)
    }

    @Test
    fun test_touch_down_and_up_per_pointer() {
        val cache = InputCache()
        cache.onTouchDown(1f, 2f, Pointer.POINTER1)
        cache.onTouchDown(3f, 4f, Pointer.POINTER2)
        assertTrue(cache.isTouching)
        assertTrue(cache.isTouching(2))
        assertFalse(cache.isTouching(3))
        assertTrue(cache.isJustTouched(Pointer.POINTER2))
        assertTrue(cache.justTouched)
        cache.reset()
        cache.onTouchUp(1f, 2f, Pointer.POINTER1)
        assertFalse(cache.isTouching(Pointer.POINTER1))
        assertTrue(cache.isTouching(Pointer.POINTER2))
        assertTrue(cache.isTouchJustReleased(Pointer.POINTER1))
        assertFalse(cache.isJustTouched(Pointer.POINTER2))
        assertTrue(cache.isTouching)
    }

    @Test
    fun test_touch_up_without_down_is_ignored() {
        val cache = InputCache()
        cache.onTouchUp(0f, 0f, Pointer.POINTER1)
        assertFalse(cache.justTouchReleased)
        cache.onTouchDown(0f, 0f, Pointer.POINTER2)
        assertTrue(cache.isTouching)
    }

    @Test
    fun test_reset_clears_touch_flags() {
        val cache = InputCache()
        cache.onTouchDown(0f, 0f, Pointer.POINTER1)
        cache.onTouchUp(0f, 0f, Pointer.POINTER1)
        cache.reset()
        assertFalse(cache.justTouched)
        assertFalse(cache.justTouchReleased)
        assertFalse(cache.isJustTouched(Pointer.POINTER1))
        assertFalse(cache.isTouchJustReleased(Pointer.POINTER1))
    }

    @Test
    fun test_move_is_drag_while_touching_and_mouse_move_otherwise() {
        val cache = InputCache()
        val recorder = Recorder()
        cache.onMove(1f, 2f, Pointer.POINTER1)
        cache.onTouchDown(1f, 2f, Pointer.POINTER1)
        cache.onMove(3f, 4f, Pointer.POINTER1)
        cache.processEvents(listOf(recorder))
        assertEquals(listOf("move 1.0 2.0", "drag 3.0 4.0 POINTER1"), recorder.events)
    }

    @Test
    fun test_gamepad_button_press_and_release_from_raw_state() {
        val input = FakeInput()
        input.pressButton(GameButton.BUTTON0)
        assertTrue(input.cache.isGamepadButtonPressed(GameButton.BUTTON0, 0))
        assertTrue(input.cache.isGamepadButtonJustPressed(GameButton.BUTTON0, 0))
        assertTrue(input.cache.anyGamepadButtonPressed)
        input.cache.reset()
        assertFalse(input.cache.isGamepadButtonJustPressed(GameButton.BUTTON0, 0))
        input.pressButton(GameButton.BUTTON0, 0f)
        assertFalse(input.cache.isGamepadButtonPressed(GameButton.BUTTON0, 0))
        assertTrue(input.cache.isGamepadButtonJustReleased(GameButton.BUTTON0, 0))
        assertFalse(input.cache.anyGamepadButtonPressed)
    }

    @Test
    fun test_gamepad_state_is_per_gamepad() {
        val input = FakeInput()
        input.pressButton(GameButton.BUTTON1, gamepad = 1)
        assertTrue(input.cache.isGamepadButtonPressed(GameButton.BUTTON1, 1))
        assertFalse(input.cache.isGamepadButtonPressed(GameButton.BUTTON1, 0))
    }

    @Test
    fun test_analog_button_pressure_changes_release_cleanly() {
        val input = FakeInput()
        input.pressButton(GameButton.BUTTON0, 0.5f)
        input.pressButton(GameButton.BUTTON0, 1f)
        input.pressButton(GameButton.BUTTON0, 0f)
        assertFalse(input.cache.anyGamepadButtonPressed)
    }

    @Test
    fun test_analog_button_pressure_change_is_not_just_pressed_again() {
        val input = FakeInput()
        input.pressButton(GameButton.BUTTON0, 0.5f)
        input.cache.reset()
        input.pressButton(GameButton.BUTTON0, 1f)
        assertFalse(input.cache.isGamepadButtonJustPressed(GameButton.BUTTON0, 0))
        assertFalse(input.cache.anyGamepadButtonsJustPressed)
        assertTrue(input.cache.isGamepadButtonPressed(GameButton.BUTTON0, 0))
    }

    @Test
    fun test_gamepad_button_events_include_pressure_and_index() {
        val input = FakeInput()
        val recorder = Recorder()
        input.pressButton(GameButton.BUTTON2, 0.5f, gamepad = 1)
        input.pressButton(GameButton.BUTTON2, 0f, gamepad = 1)
        input.cache.processEvents(listOf(recorder))
        assertEquals(listOf("pressed BUTTON2 0.5 1", "released BUTTON2 1"), recorder.events)
    }

    @Test
    fun test_unchanged_gamepad_state_emits_nothing() {
        val input = FakeInput()
        val recorder = Recorder()
        input.pressButton(GameButton.BUTTON0)
        input.pressButton(GameButton.BUTTON0)
        input.cache.updateGamepadStick(GameStick.LEFT, input.gamepads[0])
        input.cache.processEvents(listOf(recorder))
        assertEquals(listOf("pressed BUTTON0 1.0 0"), recorder.events)
    }

    @Test
    fun test_stick_moved_only_when_position_changes() {
        val input = FakeInput()
        val recorder = Recorder()
        input.setAxis(GameAxis.RX, 0.5f)
        input.setAxis(GameAxis.RY, -0.25f)
        input.cache.updateGamepadStick(GameStick.RIGHT, input.gamepads[0])
        input.cache.updateGamepadStick(GameStick.RIGHT, input.gamepads[0])
        input.cache.processEvents(listOf(recorder))
        assertEquals(listOf("stick RIGHT 0.5 -0.25 0"), recorder.events)
    }

    @Test
    fun test_trigger_changed_reports_pressure() {
        val input = FakeInput()
        val recorder = Recorder()
        val info = input.gamepads[0]
        info.rawButtonsPressed[info.mapping.buttonToIndex.getValue(GameButton.L2)] = 0.25f
        input.cache.updateGamepadTrigger(GameButton.L2, info)
        input.cache.updateGamepadTrigger(GameButton.L2, info)
        input.cache.processEvents(listOf(recorder))
        assertEquals(listOf("trigger L2 0.25 0"), recorder.events)
    }

    @Test
    fun test_reset_clears_gamepad_flags_for_all_gamepads() {
        val input = FakeInput()
        input.pressButton(GameButton.BUTTON0, gamepad = 0)
        input.pressButton(GameButton.BUTTON0, gamepad = 1)
        input.cache.reset()
        assertFalse(input.cache.isGamepadButtonJustPressed(GameButton.BUTTON0, 0))
        assertFalse(input.cache.isGamepadButtonJustPressed(GameButton.BUTTON0, 1))
        assertFalse(input.cache.anyGamepadButtonsJustPressed)
        assertTrue(input.cache.anyGamepadButtonPressed)
    }
}
