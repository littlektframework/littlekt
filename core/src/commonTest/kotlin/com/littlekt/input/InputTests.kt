package com.littlekt.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InputTests {

    @Test
    fun test_pointer_lookup_by_index() {
        assertSame(Pointer.POINTER1, Pointer[0])
        assertSame(Pointer.POINTER520, Pointer[19])
        Pointer.entries.forEach { assertSame(it, Pointer[it.index]) }
    }

    @Test
    fun test_pointer_mouse_aliases() {
        assertSame(Pointer.POINTER1, Pointer.MOUSE_LEFT)
        assertSame(Pointer.POINTER2, Pointer.MOUSE_RIGHT)
        assertSame(Pointer.POINTER3, Pointer.MOUSE_MIDDLE)
    }

    @Test
    fun test_game_button_aliases() {
        assertSame(GameButton.L2, GameButton.LEFT_TRIGGER)
        assertSame(GameButton.R3, GameButton.RIGHT_THUMB)
        assertSame(GameButton.SELECT, GameButton.BACK)
        assertSame(GameButton.BUTTON1, GameButton.XBOX_B)
        assertSame(GameButton.BUTTON3, GameButton.PS_TRIANGLE)
        assertTrue(GameButton.BUTTONS.all { it.index < GameButton.MAX })
    }

    @Test
    fun test_standard_mapping_indices_round_trip() {
        val mapping = StandardGamepadMapping
        mapping.buttonListOrder.forEachIndexed { index, button ->
            assertEquals(index, mapping.buttonToIndex[button])
            assertSame(button, mapping.indexToButton[index])
        }
        assertEquals(1, mapping.axisToIndex[GameAxis.LY])
        assertSame(GameAxis.RX, mapping.indexToAxis[2])
    }

    @Test
    fun test_gamepad_info_reads_mapped_buttons_and_axes() {
        val info = GamepadInfo()
        info.rawButtonsPressed[6] = 0.75f
        info.rawAxes[3] = -0.5f
        assertEquals(0.75f, info[GameButton.L2])
        assertEquals(-0.5f, info[GameAxis.RY])
        assertEquals(-0.5f, info.getY(GameStick.RIGHT))
        assertEquals(0f, info.getX(GameStick.LEFT))
    }

    @Test
    fun test_unmapped_button_reads_zero() {
        val info = GamepadInfo()
        info.rawButtonsPressed.fill(1f)
        assertEquals(0f, info[GameButton.BUTTON4])
    }

    @Test
    fun test_axis_outside_raw_range_reads_zero() {
        val mapping =
            object : GamepadMapping() {
                override val id = "Short"
                override val buttonListOrder = listOf(GameButton.BUTTON0)
                override val axisListOrder = listOf(GameAxis.LX, GameAxis.LY)
            }
        val info = GamepadInfo(mapping = mapping, rawAxes = FloatArray(1) { 0.5f })
        assertEquals(0.5f, info[GameAxis.LX])
        assertEquals(0f, info[GameAxis.LY])
        assertEquals(0f, info[GameAxis.RX])
    }

    @Test
    fun test_gamepad_stick_point_is_reused() {
        val info = GamepadInfo()
        info.rawAxes[0] = 0.25f
        info.rawAxes[1] = 0.5f
        val first = info[GameStick.LEFT]
        assertEquals(0.25f, first.x)
        assertEquals(0.5f, first.y)
        info.rawAxes[0] = -1f
        val second = info[GameStick.LEFT]
        assertSame(first, second)
        assertEquals(-1f, second.x)
    }

    @Test
    fun test_key_group_helpers() {
        val input = FakeInput()
        input.keyDown(Key.A, Key.B)
        assertTrue(input.areAnyKeysPressed(Key.C, Key.B))
        assertFalse(input.areAnyKeysPressed(Key.C, Key.D))
        assertTrue(input.areAllKeysPressed(Key.A, Key.B))
        assertFalse(input.areAllKeysPressed(Key.A, Key.C))
        assertTrue(input.areNoKeysPressed(Key.C, Key.D))
        assertFalse(input.areNoKeysPressed(Key.C, Key.A))
    }

    @Test
    fun test_input_processor_dsl_registers_processor() {
        val input = FakeInput()
        val keys = mutableListOf<Key>()
        val processor = input.inputProcessor { onKeyDown { keys += it } }
        assertEquals(listOf<InputProcessor>(processor), input.inputProcessors)
        input.keyDown(Key.Q)
        input.drain()
        assertEquals(listOf(Key.Q), keys)
    }

    @Test
    fun test_builder_unhandled_variants_return_false() {
        val calls = mutableListOf<String>()
        val processor =
            InputProcessBuilder()
                .apply {
                    onKeyUp { calls += "keyUp $it" }
                    onKeyRepeat { calls += "repeat $it" }
                    onCharTyped { calls += "char $it" }
                    onTouchDown { x, y, p -> calls += "down ${x.str()} ${y.str()} $p" }
                    onTouchUp { x, y, p -> calls += "up ${x.str()} ${y.str()} $p" }
                    onTouchDragged { x, y, p -> calls += "drag ${x.str()} ${y.str()} $p" }
                    onMouseMoved { x, y -> calls += "move ${x.str()} ${y.str()}" }
                    onScrolled { x, y -> calls += "scroll ${x.str()} ${y.str()}" }
                    onGamepadButtonPressed { b, p, g -> calls += "pressed $b ${p.str()} $g" }
                    onGamepadButtonReleased { b, g -> calls += "released $b $g" }
                    onGamepadJoystickMoved { s, x, y, g -> calls += "stick $s ${x.str()} ${y.str()} $g" }
                    onGamepadTriggerChanged { b, p, g -> calls += "trigger $b ${p.str()} $g" }
                }
                .build()
        assertFalse(processor.keyUp(Key.A))
        assertFalse(processor.keyRepeat(Key.A))
        assertFalse(processor.charTyped('x'))
        assertFalse(processor.touchDown(1f, 2f, Pointer.POINTER1))
        assertFalse(processor.touchUp(1f, 2f, Pointer.POINTER1))
        assertFalse(processor.touchDragged(1f, 2f, Pointer.POINTER1))
        assertFalse(processor.mouseMoved(1f, 2f))
        assertFalse(processor.scrolled(0f, 1f))
        assertFalse(processor.gamepadButtonPressed(GameButton.BUTTON0, 1f, 0))
        assertFalse(processor.gamepadButtonReleased(GameButton.BUTTON0, 0))
        assertFalse(processor.gamepadJoystickMoved(GameStick.LEFT, 0.5f, 0f, 1))
        assertFalse(processor.gamepadTriggerChanged(GameButton.R2, 0.5f, 1))
        assertEquals(12, calls.size)
        assertEquals("stick LEFT 0.5 0.0 1", calls[10])
    }

    @Test
    fun test_builder_handled_when_any_handler_returns_true() {
        val calls = mutableListOf<Int>()
        val processor =
            InputProcessBuilder()
                .apply {
                    onKeyDownHandle {
                        calls += 1
                        false
                    }
                    onKeyDownHandle {
                        calls += 2
                        true
                    }
                    onKeyDown { calls += 3 }
                }
                .build()
        assertTrue(processor.keyDown(Key.A))
        assertEquals(listOf(1, 2, 3), calls)
    }

    @Test
    fun test_builder_without_handlers_is_unhandled() {
        val processor = InputProcessBuilder().build()
        assertFalse(processor.keyDown(Key.A))
        assertFalse(processor.touchDown(0f, 0f, Pointer.POINTER1))
        assertFalse(processor.gamepadButtonPressed(GameButton.BUTTON0, 1f, 0))
    }

    @Test
    fun test_builder_handle_variants_report_handled() {
        val processor =
            InputProcessBuilder()
                .apply {
                    onTouchDownHandle { _, _, p -> p == Pointer.POINTER2 }
                    onScrolledHandle { _, y -> y > 0f }
                    onGamepadButtonReleasedHandle { _, g -> g == 1 }
                }
                .build()
        assertTrue(processor.touchDown(0f, 0f, Pointer.POINTER2))
        assertFalse(processor.touchDown(0f, 0f, Pointer.POINTER1))
        assertTrue(processor.scrolled(0f, 1f))
        assertFalse(processor.scrolled(0f, -1f))
        assertTrue(processor.gamepadButtonReleased(GameButton.BUTTON0, 1))
    }
}
