package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.InputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseButtonTests {

    private class TestButton : BaseButton()

    private fun TestButton.click() {
        uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_DOWN })
        uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_UP })
    }

    @Test
    fun test_toggle_button_toggles_on_and_off() {
        val button = TestButton().apply { toggleMode = true }
        val toggles = mutableListOf<Boolean>()
        button.onToggled += { toggles += it }
        button.click()
        assertTrue(button.pressed)
        button.click()
        assertFalse(button.pressed)
        assertEquals(listOf(true, false), toggles)
    }

    @Test
    fun test_grouped_toggle_buttons_stay_pressed_and_unpress_others() {
        val group = ButtonGroup()
        val a = TestButton().apply {
            toggleMode = true
            setButtonGroup(group)
        }
        val b = TestButton().apply {
            toggleMode = true
            setButtonGroup(group)
        }
        a.click()
        assertTrue(a.pressed)
        a.click()
        assertTrue(a.pressed)
        b.click()
        assertTrue(b.pressed)
        assertFalse(a.pressed)
    }

    @Test
    fun test_normal_button_emits_pressed_on_release() {
        val button = TestButton()
        var presses = 0
        button.onPressed += { presses++ }
        button.click()
        assertEquals(1, presses)
        assertFalse(button.pressed)
    }

    @Test
    fun test_toggle_ignores_other_events_while_held() {
        val button = TestButton().apply { toggleMode = true }
        button.uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_DOWN })
        button.uiInput(InputEvent<Any>().apply { type = InputEvent.Type.MOUSE_ENTER })
        button.uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_DRAGGED })
        assertFalse(button.pressed)
        button.uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_UP })
        assertTrue(button.pressed)
    }
}
