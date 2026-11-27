package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.InputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckBoxTests {

    init {
        useTestTheme()
    }

    private fun CheckBox.click() {
        uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_DOWN })
        uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_UP })
    }

    @Test
    fun test_click_toggles_checked_and_emits() {
        val box = CheckBox()
        val toggles = mutableListOf<Boolean>()
        box.onToggled += { toggles += it }
        assertFalse(box.checked)
        box.click()
        assertTrue(box.checked)
        box.click()
        assertFalse(box.checked)
        assertEquals(listOf(true, false), toggles)
    }

    @Test
    fun test_setting_checked_emits_only_on_change() {
        val box = CheckBox()
        var toggles = 0
        box.onToggled += { toggles++ }
        box.checked = true
        box.checked = true
        assertTrue(box.checked)
        assertEquals(1, toggles)
    }

    @Test
    fun test_disabled_ignores_clicks() {
        val box = CheckBox().apply { disabled = true }
        box.click()
        assertFalse(box.checked)
    }
}
