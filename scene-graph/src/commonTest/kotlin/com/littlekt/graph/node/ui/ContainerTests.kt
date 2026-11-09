package com.littlekt.graph.node.ui

import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.resource.AlignMode
import com.littlekt.graph.node.ui.Control.SizeFlag
import com.littlekt.math.geom.Angle
import kotlin.test.Test
import kotlin.test.assertEquals

class ContainerTests {

    private class TestVBox : VBoxContainer() {
        fun layout() = onSortChildren()
    }

    private class TestHBox : HBoxContainer() {
        fun layout() = onSortChildren()
    }

    private class TestCenter : CenterContainer() {
        fun layout() = onSortChildren()
    }

    private class TestPadded : PaddedContainer() {
        fun layout() = onSortChildren()
    }

    private class TestPanel : PanelContainer() {
        fun layout() = onSortChildren()
    }

    private fun Container.child(
        minWidth: Float = 0f,
        minHeight: Float = 0f,
        block: Control.() -> Unit = {},
    ): Control =
        Control()
            .apply {
                this.minWidth = minWidth
                this.minHeight = minHeight
                block()
            }
            .addTo(this)

    private fun assertRect(control: Control, x: Float, y: Float, width: Float, height: Float) {
        assertEquals(listOf(x, y, width, height), listOf(control.x, control.y, control.width, control.height))
    }

    private fun padded(left: Int, right: Int, top: Int, bottom: Int) =
        TestPadded().apply {
            constantOverrides.putAll(
                mapOf(
                    PaddedContainer.themeVars.paddingLeft to left,
                    PaddedContainer.themeVars.paddingRight to right,
                    PaddedContainer.themeVars.paddingTop to top,
                    PaddedContainer.themeVars.paddingBottom to bottom,
                )
            )
        }

    @Test
    fun test_vbox_min_size_sums_heights_with_separation() {
        val box = TestVBox().apply { separation = 4 }
        box.child(5f, 10f)
        box.child(30f, 20f)
        assertEquals(30f, box.combinedMinWidth)
        assertEquals(34f, box.combinedMinHeight)
    }

    @Test
    fun test_hbox_min_size_sums_widths_with_separation() {
        val box = TestHBox().apply { separation = 4 }
        box.child(10f, 5f)
        box.child(20f, 30f)
        box.child(1f, 1f)
        assertEquals(39f, box.combinedMinWidth)
        assertEquals(30f, box.combinedMinHeight)
    }

    @Test
    fun test_box_min_size_ignores_hidden_and_disabled_children() {
        val box = TestHBox().apply { separation = 4 }
        box.child(10f, 5f)
        val hidden = box.child(20f, 30f)
        val disabled = box.child(7f, 7f)
        hidden.visible = false
        disabled.enabled = false
        assertEquals(10f, box.combinedMinWidth)
        assertEquals(5f, box.combinedMinHeight)
        hidden.visible = true
        assertEquals(34f, box.combinedMinWidth)
    }

    @Test
    fun test_vbox_lays_out_children_top_down() {
        val box = TestVBox().apply { separation = 10 }
        val first = box.child(minHeight = 20f)
        val second = box.child(minHeight = 30f)
        box.size(50f, 100f)
        box.layout()
        assertRect(first, 0f, 80f, 50f, 20f)
        assertRect(second, 0f, 40f, 50f, 30f)
    }

    @Test
    fun test_vbox_expand_child_takes_remaining_space() {
        val box = TestVBox().apply { separation = 10 }
        val first = box.child(minHeight = 20f)
        val second = box.child(minHeight = 30f) { verticalSizing = SizeFlag.FILL_EXPAND }
        box.size(50f, 100f)
        box.layout()
        assertRect(first, 0f, 80f, 50f, 20f)
        assertRect(second, 0f, 0f, 50f, 70f)
    }

    @Test
    fun test_hbox_lays_out_children_left_to_right() {
        val box = TestHBox().apply { separation = 5 }
        val first = box.child(minWidth = 20f)
        val second = box.child(minWidth = 30f)
        box.size(100f, 40f)
        box.layout()
        assertRect(first, 0f, 0f, 20f, 40f)
        assertRect(second, 25f, 0f, 30f, 40f)
    }

    @Test
    fun test_hbox_stretch_ratio_splits_space() {
        val box = TestHBox().apply { separation = 0 }
        val first = box.child { horizontalSizing = SizeFlag.FILL_EXPAND }
        val second = box.child {
            horizontalSizing = SizeFlag.FILL_EXPAND
            stretchRatio = 3f
        }
        box.size(100f, 10f)
        box.layout()
        assertRect(first, 0f, 0f, 25f, 10f)
        assertRect(second, 25f, 0f, 75f, 10f)
    }

    @Test
    fun test_hbox_expand_respects_min_size() {
        val box = TestHBox().apply { separation = 0 }
        val first = box.child(minWidth = 80f) { horizontalSizing = SizeFlag.FILL_EXPAND }
        val second = box.child { horizontalSizing = SizeFlag.FILL_EXPAND }
        box.size(100f, 10f)
        box.layout()
        assertRect(first, 0f, 0f, 80f, 10f)
        assertRect(second, 80f, 0f, 20f, 10f)
    }

    @Test
    fun test_hbox_align_center_and_end() {
        val box = TestHBox().apply { separation = 0 }
        val first = box.child(minWidth = 20f)
        val second = box.child(minWidth = 20f)
        box.size(100f, 10f)
        box.align = AlignMode.CENTER
        box.layout()
        assertEquals(30f, first.x)
        assertEquals(50f, second.x)
        box.align = AlignMode.END
        box.layout()
        assertEquals(60f, first.x)
        assertEquals(80f, second.x)
    }

    @Test
    fun test_vbox_align_end_moves_children_down() {
        val box = TestVBox().apply {
            separation = 0
            align = AlignMode.END
        }
        val child = box.child(minHeight = 20f)
        box.size(10f, 100f)
        box.layout()
        assertRect(child, 0f, 0f, 10f, 20f)
    }

    @Test
    fun test_hbox_hidden_child_does_not_take_separation() {
        val box = TestHBox().apply { separation = 10 }
        val first = box.child { horizontalSizing = SizeFlag.FILL_EXPAND }
        val hidden = box.child(minWidth = 20f)
        hidden.visible = false
        box.size(100f, 10f)
        box.layout()
        assertRect(first, 0f, 0f, 100f, 10f)
    }

    @Test
    fun test_fit_child_shrink_flags_horizontal() {
        val box = TestVBox().apply { separation = 0 }
        val begin = box.child(minWidth = 20f, minHeight = 10f) { horizontalSizing = SizeFlag.SHRINK_BEGIN }
        val center = box.child(minWidth = 20f, minHeight = 10f) { horizontalSizing = SizeFlag.SHRINK_CENTER }
        val end = box.child(minWidth = 20f, minHeight = 10f) { horizontalSizing = SizeFlag.SHRINK_END }
        box.size(51f, 30f)
        box.layout()
        assertRect(begin, 0f, 20f, 20f, 10f)
        assertRect(center, 15f, 10f, 20f, 10f)
        assertRect(end, 31f, 0f, 20f, 10f)
    }

    @Test
    fun test_fit_child_shrink_flags_vertical() {
        val box = TestHBox().apply { separation = 0 }
        val begin = box.child(minWidth = 10f, minHeight = 20f) { verticalSizing = SizeFlag.SHRINK_BEGIN }
        val center = box.child(minWidth = 10f, minHeight = 20f) { verticalSizing = SizeFlag.SHRINK_CENTER }
        val end = box.child(minWidth = 10f, minHeight = 20f) { verticalSizing = SizeFlag.SHRINK_END }
        box.size(30f, 50f)
        box.layout()
        assertRect(begin, 0f, 0f, 10f, 20f)
        assertRect(center, 10f, 15f, 10f, 20f)
        assertRect(end, 20f, 30f, 10f, 20f)
    }

    @Test
    fun test_children_sizing_overrides_and_resets_on_removal() {
        val box = TestHBox().apply { separation = 0 }
        val child = box.child()
        box.childrenVerticalSizing = SizeFlag.SHRINK_CENTER
        assertEquals(SizeFlag.SHRINK_CENTER, child.verticalSizing)
        val late = box.child()
        assertEquals(SizeFlag.SHRINK_CENTER, late.verticalSizing)
        box.removeChild(late)
        assertEquals(SizeFlag.FILL, late.verticalSizing)
        box.childrenVerticalSizing = null
        assertEquals(SizeFlag.FILL, child.verticalSizing)
    }

    @Test
    fun test_layout_resets_child_rotation_and_scale() {
        val box = TestHBox().apply { separation = 0 }
        val child = box.child(minWidth = 10f)
        child.rotation = Angle(1f)
        child.scale(2f, 2f)
        box.size(10f, 10f)
        box.layout()
        assertEquals(0f, child.rotation.radians)
        assertEquals(1f, child.scaleX)
        assertEquals(1f, child.scaleY)
    }

    @Test
    fun test_center_container_centers_child() {
        val center = TestCenter()
        val child = center.child(20f, 10f)
        center.size(100f, 81f)
        center.layout()
        assertRect(child, 40f, 35f, 20f, 10f)
        assertEquals(20f, center.combinedMinWidth)
        assertEquals(10f, center.combinedMinHeight)
    }

    @Test
    fun test_padded_container_min_size_and_layout() {
        val padded = padded(left = 5, right = 10, top = 3, bottom = 7)
        val child = padded.child(20f, 10f)
        assertEquals(35f, padded.combinedMinWidth)
        assertEquals(20f, padded.combinedMinHeight)
        padded.size(100f, 80f)
        padded.layout()
        assertRect(child, 5f, 7f, 85f, 70f)
    }

    @Test
    fun test_padded_container_padding_can_be_reset_to_zero() {
        val padded = padded(left = 5, right = 0, top = 0, bottom = 0)
        padded.child(20f, 10f)
        assertEquals(25f, padded.combinedMinWidth)
        padded.paddingLeft = 0
        assertEquals(0, padded.paddingLeft)
        assertEquals(20f, padded.combinedMinWidth)
    }

    @Test
    fun test_padded_container_padding_sets_all_sides() {
        val padded = padded(left = 0, right = 0, top = 0, bottom = 0)
        padded.padding(4)
        assertEquals(4, padded.paddingLeft)
        assertEquals(4, padded.paddingRight)
        assertEquals(4, padded.paddingTop)
        assertEquals(4, padded.paddingBottom)
        assertEquals(8f, padded.combinedMinWidth)
    }

    @Test
    fun test_panel_container_uses_panel_margins() {
        val panel = TestPanel()
        panel.panel =
            TestDrawable(
                minWidth = 4f,
                minHeight = 6f,
                marginLeft = 1f,
                marginRight = 2f,
                marginTop = 3f,
                marginBottom = 4f,
            )
        val child = panel.child(20f, 10f)
        assertEquals(24f, panel.combinedMinWidth)
        assertEquals(16f, panel.combinedMinHeight)
        panel.size(100f, 50f)
        panel.layout()
        assertRect(child, 1f, 4f, 97f, 43f)
    }
}
