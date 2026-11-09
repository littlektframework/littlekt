package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.InputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrollBarTests {

    private fun <T : ScrollBar> T.withDrawables(
        increment: Drawable = TestDrawable(minWidth = 10f, minHeight = 10f),
        decrement: Drawable = TestDrawable(minWidth = 10f, minHeight = 10f),
        scroll: Drawable = TestDrawable(),
        grabber: Drawable = TestDrawable(),
    ): T = apply {
        drawableOverrides.putAll(
            mapOf(
                ScrollBar.themeVars.incrementIcon to increment,
                ScrollBar.themeVars.decrementIcon to decrement,
                ScrollBar.themeVars.scroll to scroll,
                ScrollBar.themeVars.grabber to grabber,
            )
        )
    }

    private fun event(type: InputEvent.Type, local: Float = 0f, scrollY: Float = 0f) =
        InputEvent<Any>().apply {
            this.type = type
            localX = local
            localY = local
            scrollAmountY = scrollY
        }

    @Test
    fun test_vertical_min_size() {
        val bar =
            VScrollBar()
                .withDrawables(
                    increment = TestDrawable(minWidth = 8f, minHeight = 10f),
                    decrement = TestDrawable(minWidth = 8f, minHeight = 12f),
                    scroll = TestDrawable(minWidth = 6f, minHeight = 3f),
                    grabber = TestDrawable(minWidth = 4f, minHeight = 5f),
                )
        assertEquals(8f, bar.combinedMinWidth)
        assertEquals(30f, bar.combinedMinHeight)
    }

    @Test
    fun test_horizontal_min_size() {
        val bar =
            HScrollBar()
                .withDrawables(
                    increment = TestDrawable(minWidth = 10f, minHeight = 8f),
                    decrement = TestDrawable(minWidth = 12f, minHeight = 8f),
                    scroll = TestDrawable(minWidth = 3f, minHeight = 9f),
                    grabber = TestDrawable(minWidth = 5f, minHeight = 4f),
                )
        assertEquals(30f, bar.combinedMinWidth)
        assertEquals(9f, bar.combinedMinHeight)
    }

    @Test
    fun test_scroll_wheel_moves_by_quarter_page() {
        val bar = VScrollBar().withDrawables().apply {
            page = 20f
            value = 50f
        }
        val down = event(InputEvent.Type.SCROLLED, scrollY = 1f)
        bar.uiInput(down)
        assertEquals(45f, bar.value)
        assertTrue(down.handled)
        bar.uiInput(event(InputEvent.Type.SCROLLED, scrollY = -1f))
        assertEquals(50f, bar.value)
    }

    @Test
    fun test_vertical_increment_and_decrement_buttons_step() {
        val bar = VScrollBar().withDrawables().apply {
            size(10f, 100f)
            value = 50f
        }
        bar.uiInput(event(InputEvent.Type.TOUCH_DOWN, local = 5f))
        assertEquals(51f, bar.value)
        bar.uiInput(event(InputEvent.Type.TOUCH_UP))
        bar.uiInput(event(InputEvent.Type.TOUCH_DOWN, local = 95f))
        assertEquals(50f, bar.value)
    }

    @Test
    fun test_horizontal_decrement_and_increment_buttons_step() {
        val bar = HScrollBar().withDrawables().apply {
            size(100f, 10f)
            value = 50f
        }
        bar.uiInput(event(InputEvent.Type.TOUCH_DOWN, local = 5f))
        assertEquals(49f, bar.value)
        bar.uiInput(event(InputEvent.Type.TOUCH_DOWN, local = 95f))
        assertEquals(50f, bar.value)
    }

    @Test
    fun test_vertical_click_below_grabber_pages() {
        val bar = VScrollBar().withDrawables().apply {
            size(10f, 100f)
            page = 20f
        }
        bar.uiInput(event(InputEvent.Type.TOUCH_DOWN, local = 50f))
        assertEquals(20f, bar.value)
    }
}
