package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.VAlign
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContainerScopeTests {

    private class TestHBox : HBoxContainer() {
        fun layout() = onSortChildren()
    }

    private class TestVBox : VBoxContainer() {
        fun layout() = onSortChildren()
    }

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = Theme()
    }

    private fun row(block: TestHBox.() -> Unit) =
        TestHBox().apply {
            separation = 0
            size(100f, 100f)
            block()
            layout()
        }

    private fun column(block: TestVBox.() -> Unit) =
        TestVBox().apply {
            separation = 0
            size(100f, 100f)
            block()
            layout()
        }

    @Test
    fun test_row_weight_shares_free_width() {
        lateinit var a: Control
        lateinit var b: Control
        row {
            a = control(modifier = Modifier.weight(1f))
            b = control(modifier = Modifier.weight(3f))
        }
        assertEquals(25f, a.width)
        assertEquals(75f, b.width)
    }

    @Test
    fun test_row_weight_without_fill_keeps_min_width() {
        lateinit var a: Control
        row { a = control(modifier = Modifier.weight(fill = false).minSize(10f, 10f)) }
        assertEquals(10f, a.width)
        assertEquals(Control.SizeFlag.EXPAND, a.horizontalSizing)
    }

    @Test
    fun test_row_align_places_child_vertically() {
        lateinit var top: Control
        lateinit var center: Control
        lateinit var bottom: Control
        row {
            top = control(modifier = Modifier.align(VAlign.TOP).minSize(10f, 20f))
            center = control(modifier = Modifier.align(VAlign.CENTER).minSize(10f, 20f))
            bottom = control(modifier = Modifier.align(VAlign.BOTTOM).minSize(10f, 20f))
        }
        assertEquals(20f, top.height)
        assertEquals(80f, top.y)
        assertEquals(40f, center.y)
        assertEquals(0f, bottom.y)
    }

    @Test
    fun test_row_fill_height_stretches_child() {
        lateinit var a: Control
        row { a = control(modifier = Modifier.fillHeight().minSize(10f, 20f)) }
        assertEquals(100f, a.height)
    }

    @Test
    fun test_outer_modifier_wins_on_same_control() {
        lateinit var a: Control
        row { a = control(modifier = Modifier.fillHeight().align(VAlign.TOP)) }
        assertEquals(Control.SizeFlag.FILL, a.verticalSizing)
    }

    @Test
    fun test_column_weight_shares_free_height() {
        lateinit var a: Control
        lateinit var b: Control
        column {
            a = control(modifier = Modifier.weight(3f))
            b = control(modifier = Modifier.weight(1f))
        }
        assertEquals(75f, a.height)
        assertEquals(25f, b.height)
    }

    @Test
    fun test_column_align_places_child_horizontally() {
        lateinit var left: Control
        lateinit var center: Control
        lateinit var right: Control
        column {
            left = control(modifier = Modifier.align(HAlign.LEFT).minSize(30f, 10f))
            center = control(modifier = Modifier.align(HAlign.CENTER).minSize(30f, 10f))
            right = control(modifier = Modifier.align(HAlign.RIGHT).minSize(30f, 10f))
        }
        assertEquals(30f, left.width)
        assertEquals(0f, left.x)
        assertEquals(35f, center.x)
        assertEquals(70f, right.x)
    }

    @Test
    fun test_nested_column_uses_its_own_scope() {
        lateinit var label: Control
        row {
            column(modifier = Modifier.weight()) {
                label = control(modifier = Modifier.align(HAlign.RIGHT))
            }
        }
        assertEquals(Control.SizeFlag.SHRINK_END, label.horizontalSizing)
    }
}
