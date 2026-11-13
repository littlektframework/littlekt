package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.node.resource.ThemePalette
import com.littlekt.graph.node.resource.theme
import com.littlekt.graphics.Color
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThemeTests {

    private val defaultDrawable = TestDrawable()

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = theme {
            type<Control> { drawables["bg"] = defaultDrawable }
            type<Control> { colors["fontColor"] = Color.WHITE }
        }
    }

    @Test
    fun test_builder_maps_type_by_class_name() {
        val drawable = TestDrawable()
        val built = theme {
            type<Button> {
                drawables[Button.themeVars.normal] = drawable
                colors[Button.themeVars.fontColor] = Color.RED
                constants["padding"] = 4
            }
        }
        assertSame(drawable, built.drawables["Button"]?.get(Button.themeVars.normal))
        assertEquals(Color.RED, built.colors["Button"]?.get(Button.themeVars.fontColor))
        assertEquals(4, built.constants["Button"]?.get("padding"))
    }

    @Test
    fun test_builder_type_by_name() {
        val built = theme { type("VScrollBar") { constants["size"] = 8 } }
        assertEquals(8, built.constants["VScrollBar"]?.get("size"))
    }

    @Test
    fun test_builder_skips_empty_maps() {
        val built = theme { type<Label> { colors[Label.themeVars.fontColor] = Color.WHITE } }
        assertNull(built.drawables["Label"])
        assertTrue(built.colors.containsKey("Label"))
    }

    @Test
    fun test_builder_keeps_base_values_and_overrides() {
        val base = theme {
            type<Button> {
                colors[Button.themeVars.fontColor] = Color.WHITE
                colors[Button.themeVars.fontColorDisabled] = Color.GRAY
            }
        }
        val built = theme(base) {
            type<Button> { colors[Button.themeVars.fontColor] = Color.RED }
        }
        assertEquals(Color.RED, built.colors["Button"]?.get(Button.themeVars.fontColor))
        assertEquals(Color.GRAY, built.colors["Button"]?.get(Button.themeVars.fontColorDisabled))
        assertEquals(Color.WHITE, base.colors["Button"]?.get(Button.themeVars.fontColor))
    }

    @Test
    fun test_cached_theme_value_updates_when_added_to_themed_parent() {
        val child = Control()
        assertSame(defaultDrawable, child.getThemeDrawable("bg"))
        val parentDrawable = TestDrawable()
        val parent = Control()
        parent.theme = theme { type<Control> { drawables["bg"] = parentDrawable } }
        parent.addChild(child)
        assertSame(parentDrawable, child.getThemeDrawable("bg"))
        parent.removeChild(child)
        assertSame(defaultDrawable, child.getThemeDrawable("bg"))
    }

    @Test
    fun test_cached_theme_value_updates_for_grandchildren() {
        val grandchild = Control()
        val child = Control().apply { addChild(grandchild) }
        assertSame(defaultDrawable, grandchild.getThemeDrawable("bg"))
        val parentDrawable = TestDrawable()
        val parent = Control()
        parent.theme = theme { type<Control> { drawables["bg"] = parentDrawable } }
        parent.addChild(child)
        assertSame(parentDrawable, grandchild.getThemeDrawable("bg"))
    }

    @Test
    fun test_has_theme_color() {
        val control = Control()
        assertTrue(control.hasThemeColor("fontColor"))
        assertFalse(control.hasThemeColor("fontColorDisabled"))
        control.colorOverrides["fontColorDisabled"] = Color.GRAY
        assertTrue(control.hasThemeColor("fontColorDisabled"))
        val parent = Control()
        parent.theme = theme { type<Control> { colors["outline"] = Color.RED } }
        parent.addChild(control)
        assertTrue(control.hasThemeColor("outline"))
    }

    @Test
    fun test_palette_to_linear_darkens_colors() {
        val linear = ThemePalette.DARK.toLinear()
        assertTrue(linear.surface.r < ThemePalette.DARK.surface.r)
        assertEquals(ThemePalette.DARK.surface.a, linear.surface.a)
        assertNotEquals(ThemePalette.DARK, linear)
    }

    @Test
    fun test_typed_scopes_set_theme_values() {
        val drawable = TestDrawable()
        val built = theme {
            button {
                normal = drawable
                fontColor = Color.RED
            }
            scrollBars { grabber = drawable }
            column { separation = 8 }
            paddedContainer { padding(4, 2) }
        }
        assertSame(drawable, built.drawables["Button"]?.get(Button.themeVars.normal))
        assertEquals(Color.RED, built.colors["Button"]?.get(Button.themeVars.fontColor))
        assertSame(drawable, built.drawables["VScrollBar"]?.get(ScrollBar.themeVars.grabber))
        assertSame(drawable, built.drawables["HScrollBar"]?.get(ScrollBar.themeVars.grabber))
        assertEquals(8, built.constants["VBoxContainer"]?.get(BoxContainer.themeVars.separation))
        val padded = built.constants["PaddedContainer"]
        assertEquals(4, padded?.get(PaddedContainer.themeVars.paddingLeft))
        assertEquals(2, padded?.get(PaddedContainer.themeVars.paddingTop))
    }

    @Test
    fun test_typed_scope_null_unsets_value() {
        val base = theme { label { fontColor = Color.RED } }
        val built = theme(base) { label { fontColor = null } }
        assertNull(built.colors["Label"])
    }

    @Test
    fun test_reparent_notifies_theme_changed() {
        val child = ThemeWatcher()
        assertEquals(Color.WHITE, child.getThemeColor("fontColor"))
        val parent = Control().apply { theme = theme { type<ThemeWatcher> { colors["fontColor"] = Color.RED } } }
        parent.addChild(child)
        assertTrue(child.changes > 0)
        assertEquals(Color.RED, child.seen)
    }

    private class ThemeWatcher : Control() {
        var changes = 0
        var seen: Color? = null

        override fun onThemeChanged() {
            changes++
            seen = getThemeColor("fontColor")
        }
    }
}
