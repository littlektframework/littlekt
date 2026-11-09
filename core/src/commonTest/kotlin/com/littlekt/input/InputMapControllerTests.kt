package com.littlekt.input

import com.littlekt.input.InputMapController.InputMode
import com.littlekt.input.InputMapController.KeyModifier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InputMapControllerTests {

    private enum class Signal {
        JUMP,
        SAVE,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        HORIZONTAL,
        VERTICAL,
        MOVE,
        FIRE,
    }

    private class Recorder(val handles: Boolean = false) : InputMapProcessor<Signal> {
        val events = mutableListOf<String>()

        override fun onActionDown(inputType: Signal): Boolean {
            events += "down $inputType"
            return handles
        }

        override fun onActionUp(inputType: Signal): Boolean {
            events += "up $inputType"
            return handles
        }

        override fun onActionRepeat(inputType: Signal): Boolean {
            events += "repeat $inputType"
            return handles
        }

        override fun onActionChange(inputType: Signal, pressure: Float): Boolean {
            events += "change $inputType $pressure"
            return handles
        }
    }

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun controller(input: FakeInput) =
        InputMapController<Signal>(input).apply {
            addBinding(Signal.JUMP, keys = listOf(Key.SPACE, Key.W), buttons = listOf(GameButton.XBOX_A))
            addBinding(Signal.LEFT, keys = listOf(Key.A), axes = listOf(GameAxis.LX))
            addBinding(Signal.RIGHT, keys = listOf(Key.D), axes = listOf(GameAxis.LX))
            addBinding(Signal.UP, keys = listOf(Key.W), axes = listOf(GameAxis.LY))
            addBinding(Signal.DOWN, keys = listOf(Key.S), axes = listOf(GameAxis.LY))
            addAxis(Signal.HORIZONTAL, Signal.RIGHT, Signal.LEFT)
            addAxis(Signal.VERTICAL, Signal.DOWN, Signal.UP)
            addVector(Signal.MOVE, Signal.RIGHT, Signal.DOWN, Signal.LEFT, Signal.UP)
        }

    @Test
    fun test_down_when_any_bound_key_is_pressed() {
        val input = FakeInput()
        val controller = controller(input)
        assertFalse(controller.down(Signal.JUMP))
        input.keyDown(Key.W)
        assertTrue(controller.down(Signal.JUMP))
    }

    @Test
    fun test_pressed_only_on_frame_key_went_down() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.SPACE)
        assertTrue(controller.pressed(Signal.JUMP))
        input.cache.reset()
        assertFalse(controller.pressed(Signal.JUMP))
        assertTrue(controller.down(Signal.JUMP))
    }

    @Test
    fun test_released_after_key_up() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.SPACE)
        input.cache.reset()
        assertFalse(controller.released(Signal.JUMP))
        input.keyUp(Key.SPACE)
        assertTrue(controller.released(Signal.JUMP))
        assertFalse(controller.down(Signal.JUMP))
    }

    @Test
    fun test_unbound_signal_is_never_down() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.SPACE)
        assertFalse(controller.down(Signal.FIRE))
        assertEquals(0f, controller.strength(Signal.FIRE))
    }

    @Test
    fun test_modifier_binding_requires_all_modifiers_and_keys() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(
            Signal.SAVE,
            keys = listOf(Key.S),
            keyModifiers = listOf(KeyModifier.CTRL, KeyModifier.SHIFT)
        )
        input.keyDown(Key.S)
        assertFalse(controller.down(Signal.SAVE))
        input.keyDown(Key.CTRL_LEFT)
        assertFalse(controller.down(Signal.SAVE))
        input.keyDown(Key.SHIFT_RIGHT)
        assertTrue(controller.down(Signal.SAVE))
        assertTrue(controller.pressed(Signal.SAVE))
    }

    @Test
    fun test_modifier_binding_requires_every_key() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(
            Signal.SAVE,
            keys = listOf(Key.S, Key.R),
            keyModifiers = listOf(KeyModifier.CTRL)
        )
        input.keyDown(Key.CTRL_RIGHT, Key.S)
        assertFalse(controller.down(Signal.SAVE))
        input.keyDown(Key.R)
        assertTrue(controller.down(Signal.SAVE))
    }

    @Test
    fun test_modifier_binding_pressed_false_when_nothing_just_pressed() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(Signal.SAVE, keys = listOf(Key.S), keyModifiers = listOf(KeyModifier.ALT))
        input.keyDown(Key.ALT_LEFT, Key.S)
        input.cache.reset()
        assertTrue(controller.down(Signal.SAVE))
        assertFalse(controller.pressed(Signal.SAVE))
    }

    @Test
    fun test_modifier_binding_released_with_right_modifier() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(Signal.SAVE, keys = listOf(Key.S), keyModifiers = listOf(KeyModifier.SHIFT))
        input.keyDown(Key.SHIFT_RIGHT, Key.S)
        input.cache.reset()
        input.keyUp(Key.S)
        assertTrue(controller.released(Signal.SAVE))
    }

    @Test
    fun test_plain_binding_still_works_while_modifier_binding_unmet() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(Signal.SAVE, keys = listOf(Key.F5))
        controller.addBinding(Signal.SAVE, keys = listOf(Key.S), keyModifiers = listOf(KeyModifier.CTRL))
        input.keyDown(Key.CTRL_LEFT, Key.F5)
        assertTrue(controller.down(Signal.SAVE))
        assertTrue(controller.pressed(Signal.SAVE))
        assertEquals(1f, controller.strength(Signal.SAVE))
    }

    @Test
    fun test_pointer_binding_down_pressed_and_released() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        controller.addBinding(Signal.FIRE, pointers = listOf(Pointer.MOUSE_LEFT))
        input.cache.onTouchDown(0f, 0f, Pointer.POINTER1)
        assertTrue(controller.down(Signal.FIRE))
        assertTrue(controller.pressed(Signal.FIRE))
        input.cache.reset()
        assertFalse(controller.pressed(Signal.FIRE))
        input.cache.onTouchUp(0f, 0f, Pointer.POINTER1)
        assertTrue(controller.released(Signal.FIRE))
        assertFalse(controller.down(Signal.FIRE))
    }

    @Test
    fun test_keyboard_strength_is_zero_or_one() {
        val input = FakeInput()
        val controller = controller(input)
        assertEquals(0f, controller.strength(Signal.RIGHT))
        input.keyDown(Key.D)
        assertEquals(1f, controller.strength(Signal.RIGHT))
        assertEquals(1f, controller.dist(Signal.RIGHT))
    }

    @Test
    fun test_keyboard_axis_is_positive_minus_negative() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.A)
        assertEquals(-1f, controller.axis(Signal.HORIZONTAL))
        input.keyDown(Key.D)
        assertEquals(0f, controller.axis(Signal.HORIZONTAL))
        input.keyUp(Key.A)
        assertEquals(1f, controller.axis(Signal.HORIZONTAL))
    }

    @Test
    fun test_axis_of_unknown_type_is_zero() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.D)
        assertEquals(0f, controller.axis(Signal.RIGHT))
    }

    @Test
    fun test_keyboard_vector_and_angle() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.D, Key.S)
        val vec = controller.vector(Signal.MOVE)
        assertEquals(1f, vec.x)
        assertEquals(1f, vec.y)
        assertNear((PI / 4).toFloat(), controller.angle(Signal.MOVE))
        assertNear((PI / 4).toFloat(), controller.angle(Signal.HORIZONTAL, Signal.VERTICAL))
        assertEquals(1f, controller.distV(Signal.MOVE))
        assertEquals(1f, controller.dist(Signal.HORIZONTAL, Signal.VERTICAL))
    }

    @Test
    fun test_vector_reuses_result_instance() {
        val input = FakeInput()
        val controller = controller(input)
        val first = controller.vector(Signal.MOVE)
        input.keyDown(Key.A)
        val second = controller.vector(Signal.MOVE)
        assertSame(first, second)
        assertEquals(-1f, second.x)
    }

    @Test
    fun test_vector_of_unknown_type_is_zero() {
        val input = FakeInput()
        val controller = controller(input)
        input.keyDown(Key.D)
        controller.vector(Signal.MOVE)
        val vec = controller.vector(Signal.JUMP)
        assertEquals(0f, vec.x)
        assertEquals(0f, vec.y)
        assertEquals(0f, controller.distV(Signal.JUMP))
        assertEquals(0f, controller.angle(Signal.JUMP))
    }

    @Test
    fun test_gamepad_mode_button_down_pressed_released() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.pressButton(GameButton.XBOX_A)
        assertTrue(controller.down(Signal.JUMP))
        assertTrue(controller.pressed(Signal.JUMP))
        input.cache.reset()
        input.pressButton(GameButton.XBOX_A, 0f)
        assertTrue(controller.released(Signal.JUMP))
        assertFalse(controller.down(Signal.JUMP))
    }

    @Test
    fun test_gamepad_mode_ignores_keys() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.gamepads[0].connected = true
        input.keyDown(Key.SPACE)
        assertFalse(controller.down(Signal.JUMP))
        assertEquals(0f, controller.strength(Signal.RIGHT))
    }

    @Test
    fun test_gamepad_down_checks_every_gamepad() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.pressButton(GameButton.XBOX_A, gamepad = 1)
        assertTrue(controller.down(Signal.JUMP))
        assertTrue(controller.pressed(Signal.JUMP))
    }

    @Test
    fun test_gamepad_down_false_without_connected_gamepads() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.pressButton(GameButton.XBOX_A)
        input.gamepads[0].connected = false
        assertFalse(controller.down(Signal.JUMP))
    }

    @Test
    fun test_gamepad_button_strength_is_pressure() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.pressButton(GameButton.XBOX_A, 0.5f)
        assertEquals(0.5f, controller.strength(Signal.JUMP))
    }

    @Test
    fun test_gamepad_axis_strength_respects_dead_zone() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.setAxis(GameAxis.LX, 0.2f)
        assertEquals(0f, controller.strength(Signal.RIGHT))
        assertEquals(0.2f, controller.strength(Signal.RIGHT, deadZone = 0.1f))
        input.setAxis(GameAxis.LX, -0.6f)
        assertEquals(-0.6f, controller.strength(Signal.RIGHT))
        assertEquals(0.6f, controller.dist(Signal.RIGHT))
    }

    @Test
    fun test_gamepad_y_axis_strength_is_inverted() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.setAxis(GameAxis.LY, -0.8f)
        assertEquals(0.8f, controller.strength(Signal.UP))
    }

    @Test
    fun test_gamepad_axis_matches_keyboard_direction() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.setAxis(GameAxis.LX, -0.5f)
        assertEquals(-0.5f, controller.axis(Signal.HORIZONTAL))
        input.setAxis(GameAxis.LX, 0.75f)
        assertEquals(0.75f, controller.axis(Signal.HORIZONTAL))
        input.setAxis(GameAxis.LX, 0.1f)
        assertEquals(0f, controller.axis(Signal.HORIZONTAL))
    }

    @Test
    fun test_gamepad_vector_uses_y_down_screen_direction() {
        val input = FakeInput()
        val controller = controller(input)
        controller.mode = InputMode.GAMEPAD
        input.setAxis(GameAxis.LX, 0.5f)
        input.setAxis(GameAxis.LY, -1f)
        val vec = controller.vector(Signal.MOVE)
        assertEquals(0.5f, vec.x)
        assertEquals(-1f, vec.y)
        assertEquals(1f, controller.distV(Signal.MOVE))
        input.setAxis(GameAxis.LY, 1f)
        assertEquals(1f, controller.vector(Signal.MOVE).y)
    }

    @Test
    fun test_mode_switches_to_gamepad_on_gamepad_events() {
        val controller = controller(FakeInput())
        controller.gamepadButtonPressed(GameButton.XBOX_A, 1f, 0)
        assertEquals(InputMode.GAMEPAD, controller.mode)
        controller.charTyped('a')
        assertEquals(InputMode.KEYBOARD, controller.mode)
        controller.gamepadTriggerChanged(GameButton.L2, 0.5f, 0)
        assertEquals(InputMode.GAMEPAD, controller.mode)
        controller.touchDown(0f, 0f, Pointer.POINTER1)
        assertEquals(InputMode.KEYBOARD, controller.mode)
    }

    @Test
    fun test_key_down_switches_to_keyboard_without_processors() {
        val controller = controller(FakeInput())
        controller.mode = InputMode.GAMEPAD
        controller.keyDown(Key.SPACE)
        assertEquals(InputMode.KEYBOARD, controller.mode)
    }

    @Test
    fun test_processor_receives_bound_key_actions() {
        val controller = controller(FakeInput())
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        controller.keyDown(Key.SPACE)
        controller.keyRepeat(Key.SPACE)
        controller.keyUp(Key.SPACE)
        controller.keyDown(Key.Z)
        assertEquals(listOf("down JUMP", "repeat JUMP", "up JUMP"), recorder.events)
    }

    @Test
    fun test_key_bound_to_many_signals_notifies_each() {
        val controller = controller(FakeInput())
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        assertFalse(controller.keyDown(Key.W))
        assertEquals(listOf("down JUMP", "down UP"), recorder.events)
    }

    @Test
    fun test_handled_action_stops_other_processors() {
        val controller = controller(FakeInput())
        val first = Recorder(handles = true)
        val second = Recorder()
        controller.addInputMapProcessor(first)
        controller.addInputMapProcessor(second)
        assertTrue(controller.keyDown(Key.W))
        assertEquals(listOf("down JUMP"), first.events)
        assertTrue(second.events.isEmpty())
    }

    @Test
    fun test_removed_processor_receives_nothing() {
        val controller = controller(FakeInput())
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        controller.removeInputMapProcessor(recorder)
        controller.keyDown(Key.SPACE)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun test_processor_receives_gamepad_actions() {
        val controller = controller(FakeInput())
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        controller.addBinding(Signal.FIRE, buttons = listOf(GameButton.R2))
        controller.gamepadButtonPressed(GameButton.XBOX_A, 1f, 0)
        controller.gamepadButtonReleased(GameButton.XBOX_A, 0)
        controller.gamepadTriggerChanged(GameButton.R2, 0.25f, 0)
        assertEquals(listOf("down JUMP", "up JUMP", "change FIRE 0.25"), recorder.events)
    }

    @Test
    fun test_processor_receives_pointer_actions() {
        val controller = InputMapController<Signal>(FakeInput())
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        controller.addBinding(Signal.FIRE, pointers = listOf(Pointer.MOUSE_RIGHT))
        controller.touchDown(0f, 0f, Pointer.MOUSE_LEFT)
        controller.touchDown(0f, 0f, Pointer.MOUSE_RIGHT)
        controller.touchUp(0f, 0f, Pointer.MOUSE_RIGHT)
        assertEquals(listOf("down FIRE", "up FIRE"), recorder.events)
    }

    @Test
    fun test_processor_receives_modifier_action_when_modifier_held() {
        val input = FakeInput()
        val controller = InputMapController<Signal>(input)
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        controller.addBinding(Signal.SAVE, keys = listOf(Key.S), keyModifiers = listOf(KeyModifier.CTRL))
        controller.keyDown(Key.S)
        assertTrue(recorder.events.isEmpty())
        input.keyDown(Key.CTRL_RIGHT, Key.S)
        controller.keyDown(Key.S)
        assertEquals(listOf("down SAVE"), recorder.events)
    }

    @Test
    fun test_controller_receives_events_through_input_queue() {
        val input = FakeInput()
        val controller = controller(input)
        val recorder = Recorder()
        controller.addInputMapProcessor(recorder)
        input.addInputProcessor(controller)
        input.keyDown(Key.D)
        input.drain()
        assertEquals(listOf("down RIGHT"), recorder.events)
        assertEquals(1f, controller.axis(Signal.HORIZONTAL))
    }
}
