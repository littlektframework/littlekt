package com.littlekt.graph.node.ui

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.resource.AlignMode
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Color
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ModifierTests {

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = Theme()
    }

    private val fill = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND

    @Test
    fun test_empty_modifier_adds_control_to_parent() {
        val parent = Node()
        assertSame(parent, parent.control().parent)
        assertSame(Modifier, Modifier then Modifier)
    }

    @Test
    fun test_elements_apply_in_order() {
        val names = mutableListOf<String>()
        val modifier =
            Modifier.then(Modifier.Element { names += "a" })
                .then(Modifier.Element { names += "b" })
                .then(Modifier.Element { names += "c" })
        Node().control(modifier = modifier)
        assertEquals(listOf("c", "b", "a"), names)
    }

    @Test
    fun test_elements_apply_before_callback() {
        val control =
            Node().control(modifier = Modifier.minSize(10f, 10f)) {
                assertEquals(10f, minWidth)
                minWidth = 20f
            }
        assertEquals(20f, control.minWidth)
        assertEquals(10f, control.minHeight)
    }

    @Test
    fun test_layout_modifiers() {
        val control =
            Node().control(modifier = Modifier.expandWidth(2f).shrinkCenter(horizontal = false))
        assertEquals(fill, control.horizontalSizing)
        assertEquals(Control.SizeFlag.SHRINK_CENTER, control.verticalSizing)
        assertEquals(2f, control.stretchRatio)
    }

    @Test
    fun test_wrapper_adds_wrapper_to_parent_and_returns_control() {
        val parent = Node()
        val control = parent.control(modifier = Modifier.padding(8))
        val wrapper = assertIs<PaddedContainer>(control.parent)
        assertSame(parent, wrapper.parent)
        assertEquals(8, wrapper.paddingLeft)
        assertEquals(8, wrapper.paddingTop)
    }

    @Test
    fun test_elements_before_wrapper_apply_to_wrapper() {
        val control = Node().control(modifier = Modifier.expandWidth().padding(4).minSize(5f, 5f))
        val wrapper = control.parent as Control
        assertEquals(fill, wrapper.horizontalSizing)
        assertEquals(Control.SizeFlag.FILL, control.horizontalSizing)
        assertEquals(5f, control.minWidth)
        assertEquals(0f, wrapper.minWidth)
    }

    @Test
    fun test_wrappers_nest_outside_in() {
        val drawable = TestDrawable()
        val control = Node().control(modifier = Modifier.background(drawable).padding(2, 4))
        val padded = assertIs<PaddedContainer>(control.parent)
        val panel = assertIs<PanelContainer>(padded.parent)
        assertSame(drawable, panel.panel)
        assertEquals(2, padded.paddingLeft)
        assertEquals(4, padded.paddingTop)
    }

    @Test
    fun test_reused_modifier_creates_new_wrappers() {
        val modifier = Modifier.padding(3)
        val parent = Node()
        val a = parent.control(modifier = modifier)
        val b = parent.control(modifier = modifier)
        assertNotSame(a.parent, b.parent)
        assertEquals(2, parent.childCount)
    }

    @Test
    fun test_custom_extension_modifier() {
        fun Modifier.named(name: String) = then(Modifier.Element { it.name = name })
        val control = Node().control(modifier = Modifier.named("custom").minSize(1f, 2f))
        assertEquals("custom", control.name)
        assertEquals(2f, control.minHeight)
    }

    @Test
    fun test_theme_override_modifiers() {
        val drawable = TestDrawable()
        val control =
            Node()
                .control(
                    modifier =
                        Modifier.fontColor(Color.RED)
                            .drawable("bg", drawable)
                            .constant("gap", 3)
                            .tint(Color.BLUE)
                )
        assertEquals(Color.RED, control.colorOverrides["fontColor"])
        assertSame(drawable, control.drawableOverrides["bg"])
        assertEquals(3, control.constantOverrides["gap"])
        assertEquals(Color.BLUE, control.color)
    }

    @Test
    fun test_visibility_modifiers() {
        val control = Node().control(modifier = Modifier.visible(false).enabled(false))
        assertFalse(control.visible)
        assertFalse(control.enabled)
    }

    @Test
    fun test_spacer_expands_and_ignores_mouse() {
        val spacer = Node().spacer()
        assertEquals(fill, spacer.horizontalSizing)
        assertEquals(fill, spacer.verticalSizing)
        assertEquals(Control.MouseFilter.IGNORE, spacer.mouseFilter)
    }

    @Test
    fun test_spacer_modifier_overrides_defaults() {
        val spacer = Node().spacer(Modifier.shrinkBegin(horizontal = false))
        assertEquals(Control.SizeFlag.SHRINK_BEGIN, spacer.verticalSizing)
        assertEquals(fill, spacer.horizontalSizing)
    }

    @Test
    fun test_box_builder_params() {
        val column = Node().column(separation = 12, align = AlignMode.CENTER)
        assertEquals(12, column.separation)
        assertEquals(AlignMode.CENTER, column.align)
        val padded = Node().padded(6, 2)
        assertEquals(6, padded.paddingLeft)
        assertEquals(2, padded.paddingBottom)
    }
}
