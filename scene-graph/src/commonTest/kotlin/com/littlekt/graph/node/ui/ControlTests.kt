package com.littlekt.graph.node.ui

import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.ui.Control.AnchorLayout
import com.littlekt.graph.node.ui.Control.SizeFlag
import com.littlekt.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ControlTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-3f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun parentOf(width: Float, height: Float) = Control().apply { size(width, height) }

    @Test
    fun test_size_sets_width_and_height() {
        val control = Control()
        control.size(30f, 40f)
        assertEquals(30f, control.width)
        assertEquals(40f, control.height)
        control.width = 10f
        control.height = 20f
        assertEquals(10f, control.width)
        assertEquals(20f, control.height)
    }

    @Test
    fun test_size_is_clamped_to_min_size() {
        val control = Control().apply {
            minWidth = 50f
            minHeight = 60f
        }
        control.size(10f, 100f)
        assertEquals(50f, control.width)
        assertEquals(100f, control.height)
    }

    @Test
    fun test_combined_min_size_uses_min_fields() {
        val control = Control()
        control.minWidth = 12f
        control.minHeight = 7f
        assertEquals(12f, control.combinedMinWidth)
        assertEquals(7f, control.combinedMinHeight)
    }

    @Test
    fun test_set_rect_sets_position_and_size() {
        val control = Control()
        control.setRect(5f, 6f, 70f, 80f)
        assertEquals(5f, control.x)
        assertEquals(6f, control.y)
        assertEquals(70f, control.width)
        assertEquals(80f, control.height)
        assertEquals(0f, control.anchorLeft)
        assertEquals(0f, control.anchorTop)
    }

    @Test
    fun test_margins_follow_position_in_parent() {
        val parent = parentOf(200f, 100f)
        val child = Control().addTo(parent)
        child.setRect(10f, 20f, 30f, 40f)
        assertEquals(10f, child.marginLeft)
        assertEquals(20f, child.marginBottom)
        assertEquals(40f, child.marginRight)
        assertEquals(60f, child.marginTop)
    }

    @Test
    fun test_anchor_full_sets_anchors_and_margins() {
        val parent = parentOf(200f, 100f)
        val child = Control().addTo(parent)
        child.anchor(AnchorLayout.FULL)
        assertEquals(0f, child.anchorLeft)
        assertEquals(0f, child.anchorBottom)
        assertEquals(1f, child.anchorRight)
        assertEquals(1f, child.anchorTop)
        assertEquals(0f, child.marginLeft)
        assertEquals(0f, child.marginRight)
        assertEquals(0f, child.marginTop)
        assertEquals(0f, child.marginBottom)
    }

    @Test
    fun test_anchor_center_offsets_margins_by_min_size() {
        val parent = parentOf(200f, 100f)
        val child = Control().apply {
            minWidth = 20f
            minHeight = 10f
        }.addTo(parent)
        child.anchor(AnchorLayout.CENTER)
        assertEquals(0.5f, child.anchorLeft)
        assertEquals(0.5f, child.anchorRight)
        assertEquals(0.5f, child.anchorTop)
        assertEquals(0.5f, child.anchorBottom)
        assertEquals(-10f, child.marginLeft)
        assertEquals(10f, child.marginRight)
        assertEquals(5f, child.marginTop)
        assertEquals(-5f, child.marginBottom)
    }

    @Test
    fun test_anchor_top_right_offsets_from_corner() {
        val parent = parentOf(200f, 100f)
        val child = Control().apply {
            minWidth = 20f
            minHeight = 10f
        }.addTo(parent)
        child.anchor(AnchorLayout.TOP_RIGHT)
        assertEquals(1f, child.anchorLeft)
        assertEquals(1f, child.anchorRight)
        assertEquals(1f, child.anchorTop)
        assertEquals(1f, child.anchorBottom)
        assertEquals(-20f, child.marginLeft)
        assertEquals(0f, child.marginRight)
        assertEquals(0f, child.marginTop)
        assertEquals(-10f, child.marginBottom)
    }

    @Test
    fun test_anchor_bottom_wide_spans_width() {
        val parent = parentOf(200f, 100f)
        val child = Control().apply { minHeight = 10f }.addTo(parent)
        child.anchor(AnchorLayout.BOTTOM_WIDE)
        assertEquals(0f, child.anchorLeft)
        assertEquals(1f, child.anchorRight)
        assertEquals(0f, child.anchorTop)
        assertEquals(0f, child.anchorBottom)
        assertEquals(10f, child.marginTop)
        assertEquals(0f, child.marginBottom)
    }

    @Test
    fun test_set_anchor_pushes_opposite_anchor() {
        val control = Control()
        control.anchorLeft = 0.75f
        assertEquals(0.75f, control.anchorRight)
        control.anchorTop = 0.5f
        control.anchorBottom = 0.8f
        assertEquals(0.8f, control.anchorTop)
    }

    @Test
    fun test_set_anchor_without_keeping_margins_preserves_edges() {
        val parent = parentOf(200f, 100f)
        val child = Control().addTo(parent)
        child.setRect(10f, 0f, 30f, 10f)
        child.setAnchor(Control.Side.LEFT, 0.5f, keepMargins = false)
        assertEquals(-90f, child.marginLeft)
        assertNear(10f, child.marginLeft + child.anchorLeft * parent.width)
    }

    @Test
    fun test_size_flags_emit_signal() {
        val control = Control()
        var calls = 0
        control.onSizeFlagsChanged += { calls++ }
        control.horizontalSizing = SizeFlag.FILL_EXPAND
        control.verticalSizing = SizeFlag.SHRINK_CENTER
        control.stretchRatio = 2f
        assertEquals(3, calls)
    }

    @Test
    fun test_size_flag_bit_ops() {
        val flag = SizeFlag.FILL or SizeFlag.EXPAND
        assertEquals(SizeFlag.FILL_EXPAND, flag)
        assertTrue(flag.isFlagSet(SizeFlag.FILL))
        assertTrue(flag.isFlagSet(SizeFlag.EXPAND))
        assertFalse(flag.isFlagSet(SizeFlag.SHRINK_END))
        assertEquals(SizeFlag.EXPAND, flag and SizeFlag.EXPAND)
        assertEquals(SizeFlag.NONE, SizeFlag.SHRINK_BEGIN)
    }

    @Test
    fun test_size_flag_set_and_parse() {
        assertEquals(SizeFlag.FILL_EXPAND, SizeFlag.set(listOf(SizeFlag.FILL, SizeFlag.EXPAND)))
        assertEquals(SizeFlag.EXPAND, SizeFlag("EXPAND"))
        assertEquals(SizeFlag.SHRINK_END, SizeFlag("SHRINK_END"))
        assertEquals(SizeFlag(6), SizeFlag("SizeFlag(6)"))
        assertEquals(SizeFlag.NONE, SizeFlag("garbage"))
    }

    @Test
    fun test_size_flag_to_string() {
        assertEquals("FILL", SizeFlag.FILL.toString())
        assertEquals("FILL_EXPAND", SizeFlag.FILL_EXPAND.toString())
        assertEquals("NONE/SHRINK_BEGIN", SizeFlag.NONE.toString())
        assertEquals("SizeFlag(6)", SizeFlag(6).toString())
        assertEquals(SizeFlag(6), SizeFlag(SizeFlag(6).toString()))
    }

    @Test
    fun test_has_point_uses_bounds() {
        val control = Control().apply { setRect(10f, 10f, 20f, 20f) }
        assertTrue(control.hasPoint(10f, 10f))
        assertTrue(control.hasPoint(29f, 29f))
        assertFalse(control.hasPoint(30f, 15f))
        assertFalse(control.hasPoint(5f, 15f))
    }

    @Test
    fun test_hit_returns_deepest_child() {
        val parent = Control().apply { setRect(0f, 0f, 100f, 100f) }
        val child = Control().addTo(parent).apply { setRect(10f, 10f, 20f, 20f) }
        assertSame(child, parent.hit(15f, 15f))
        assertSame(parent, parent.hit(50f, 50f))
        assertNull(parent.hit(150f, 50f))
    }

    @Test
    fun test_hit_respects_mouse_filter() {
        val parent = Control().apply {
            setRect(0f, 0f, 100f, 100f)
            mouseFilter = Control.MouseFilter.IGNORE
        }
        val child = Control().addTo(parent).apply { setRect(10f, 10f, 20f, 20f) }
        assertNull(parent.hit(50f, 50f))
        assertSame(child, parent.hit(15f, 15f))
        parent.mouseFilter = Control.MouseFilter.NONE
        assertNull(parent.hit(15f, 15f))
    }

    @Test
    fun test_hit_ignores_hidden_and_disabled() {
        val control = Control().apply { setRect(0f, 0f, 10f, 10f) }
        control.visible = false
        assertNull(control.hit(5f, 5f))
        control.visible = true
        control.enabled = false
        assertNull(control.hit(5f, 5f))
    }

    @Test
    fun test_color_override_takes_precedence() {
        val control = Control()
        control.colorOverrides["fontColor"] = Color.RED
        assertEquals(Color.RED, control.getThemeColor("fontColor"))
        control.constantOverrides["separation"] = 7
        assertEquals(7, control.getThemeConstant("separation"))
    }

    @Test
    fun test_has_theme_drawable_with_override() {
        val control = Control()
        control.drawableOverrides["panel"] = TestDrawable()
        assertTrue(control.hasThemeDrawable("panel"))
    }

    @Test
    fun test_find_next_valid_focus_walks_siblings() {
        val root = Control()
        val a = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        val b = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        assertSame(b, a.findNextValidFocus())
        assertSame(a, b.findNextValidFocus())
    }

    @Test
    fun test_find_previous_valid_focus_walks_siblings() {
        val root = Control()
        val a = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        val b = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        assertSame(a, b.findPreviousValidFocus())
    }

    @Test
    fun test_find_next_valid_focus_skips_non_focusable() {
        val root = Control()
        val a = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        Control().addTo(root)
        val c = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        assertSame(c, a.findNextValidFocus())
    }

    @Test
    fun test_focus_next_override_is_used() {
        val root = Control()
        val a = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        val c = Control().addTo(root).apply { focusMode = Control.FocusMode.ALL }
        a.focusNext = c
        assertSame(c, a.findNextValidFocus())
    }
}
