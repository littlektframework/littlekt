package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.InputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SpinBoxTests {

    init {
        useTestTheme()
    }

    private fun spin() = createSpinBox(10f, 0f, 100f, 1f).apply {
        arrowWidth = 18
        size(120f, 28f)
    }

    private fun SpinBox.event(type: InputEvent.Type, x: Float) {
        uiInput(InputEvent<Any>().apply {
            this.type = type
            localX = x
        })
    }

    @Test
    fun test_scrubbing_moves_one_step_per_scrub_pixels() {
        val s = spin().apply { scrubPixels = 5f }
        s.event(InputEvent.Type.TOUCH_DOWN, 60f)
        s.event(InputEvent.Type.TOUCH_DRAGGED, 85f)
        assertEquals(15f, s.value)
        s.event(InputEvent.Type.TOUCH_DRAGGED, 40f)
        assertEquals(6f, s.value)
        s.event(InputEvent.Type.TOUCH_UP, 40f)
        assertFalse(s.isEditing)
    }

    @Test
    fun test_arrows_step_and_values_clamp() {
        val s = spin()
        s.event(InputEvent.Type.TOUCH_DOWN, 110f)
        s.event(InputEvent.Type.TOUCH_UP, 110f)
        assertEquals(11f, s.value)
        s.event(InputEvent.Type.TOUCH_DOWN, 4f)
        s.event(InputEvent.Type.TOUCH_UP, 4f)
        assertEquals(10f, s.value)
        s.value = 500f
        assertEquals(100f, s.value)
    }

    @Test
    fun test_format_follows_the_step() {
        val s = createSpinBox(0.74f, 0f, 2f, 0.01f)
        assertEquals("0.74", s.formatValue())
        s.step = 0.5f
        s.value = 1.5f
        assertEquals("1.5", s.formatValue())
        val whole = createSpinBox(-3f, -10f, 10f, 1f)
        assertEquals("-3", whole.formatValue())
    }

    @Test
    fun test_disabled_ignores_input() {
        val s = spin().apply { disabled = true }
        s.event(InputEvent.Type.TOUCH_DOWN, 110f)
        s.event(InputEvent.Type.TOUCH_UP, 110f)
        assertEquals(10f, s.value)
    }
}
