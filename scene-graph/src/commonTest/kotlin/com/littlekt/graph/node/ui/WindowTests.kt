package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.InputEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowTests {

    init {
        useTestTheme()
    }

    private fun Window.event(type: InputEvent.Type, x: Float, y: Float, localY: Float) {
        uiInput(InputEvent<Any>().apply {
            this.type = type
            canvasX = x
            canvasY = y
            this.localY = localY
        })
    }

    private fun window() = Window().apply {
        titleHeight = 28
        size(200f, 150f)
    }

    @Test
    fun test_dragging_the_title_bar_moves_the_window() {
        val w = window()
        val drags = mutableListOf<Boolean>()
        w.onDrag += { drags += it }
        w.event(InputEvent.Type.TOUCH_DOWN, 50f, 140f, 140f)
        w.event(InputEvent.Type.TOUCH_DRAGGED, 70f, 130f, 0f)
        w.event(InputEvent.Type.TOUCH_UP, 70f, 130f, 0f)
        assertEquals(20f, w.x)
        assertEquals(-10f, w.y)
        assertEquals(listOf(true, false), drags)
    }

    @Test
    fun test_dragging_below_the_title_bar_does_nothing() {
        val w = window()
        w.event(InputEvent.Type.TOUCH_DOWN, 50f, 40f, 40f)
        w.event(InputEvent.Type.TOUCH_DRAGGED, 90f, 60f, 0f)
        assertEquals(0f, w.x)
        assertEquals(0f, w.y)
    }

    @Test
    fun test_not_draggable_ignores_the_title_bar() {
        val w = window().apply { draggable = false }
        w.event(InputEvent.Type.TOUCH_DOWN, 50f, 140f, 140f)
        w.event(InputEvent.Type.TOUCH_DRAGGED, 90f, 160f, 0f)
        assertEquals(0f, w.x)
    }
}
