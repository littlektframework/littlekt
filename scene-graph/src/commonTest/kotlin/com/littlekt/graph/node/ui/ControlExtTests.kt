package com.littlekt.graph.node.ui

import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.node.ui.Control.AnchorLayout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlExtTests {

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = Theme()
    }

    private fun parentOf(width: Float, height: Float) = Control().apply { size(width, height) }

    @Test
    fun test_fill_parent_with_margin_insets_every_edge() {
        val child = Control().addTo(parentOf(200f, 100f))
        child.fillParent(10f)
        assertEquals(10f, child.marginLeft)
        assertEquals(-10f, child.marginRight)
        assertEquals(-10f, child.marginTop)
        assertEquals(10f, child.marginBottom)
    }

    @Test
    fun test_anchor_margin_moves_corner_presets_inward() {
        val child = Control().apply { minSize(20f, 10f) }.addTo(parentOf(200f, 100f))
        child.anchor(AnchorLayout.TOP_RIGHT)
        val left = child.marginLeft
        val right = child.marginRight
        val top = child.marginTop
        val bottom = child.marginBottom
        child.anchor(AnchorLayout.TOP_RIGHT, 5f)
        assertEquals(left - 5f, child.marginLeft)
        assertEquals(right - 5f, child.marginRight)
        assertEquals(top - 5f, child.marginTop)
        assertEquals(bottom - 5f, child.marginBottom)
    }

    @Test
    fun test_anchor_margin_ignores_centered_edges() {
        val child = Control().apply { minSize(20f, 10f) }.addTo(parentOf(200f, 100f))
        child.centerInParent()
        val left = child.marginLeft
        val top = child.marginTop
        child.anchor(AnchorLayout.CENTER, 8f)
        assertEquals(left, child.marginLeft)
        assertEquals(top, child.marginTop)
    }

    @Test
    fun test_anchor_without_margin_resets_margin() {
        val child = Control().addTo(parentOf(200f, 100f))
        child.fillParent(10f)
        child.fillParent()
        assertEquals(0f, child.marginLeft)
        assertEquals(0f, child.marginTop)
    }

    @Test
    fun test_anchors_sets_all_four() {
        val control = Control()
        control.anchors(0.1f, 0.9f, 0.8f, 0.2f)
        assertEquals(0.1f, control.anchorLeft)
        assertEquals(0.9f, control.anchorTop)
        assertEquals(0.8f, control.anchorRight)
        assertEquals(0.2f, control.anchorBottom)
    }

    @Test
    fun test_expand_sets_both_axes_and_ratio() {
        val control = Control()
        control.expand(3f)
        val fill = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
        assertEquals(fill, control.horizontalSizing)
        assertEquals(fill, control.verticalSizing)
        assertEquals(3f, control.stretchRatio)
    }

    @Test
    fun test_shrink_sets_only_requested_axes() {
        val control = Control()
        control.shrinkEnd(vertical = false)
        assertEquals(Control.SizeFlag.SHRINK_END, control.horizontalSizing)
        assertEquals(Control.SizeFlag.FILL, control.verticalSizing)
    }
}
