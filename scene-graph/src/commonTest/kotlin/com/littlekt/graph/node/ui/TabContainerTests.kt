package com.littlekt.graph.node.ui

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.Theme
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TabContainerTests {

    @BeforeTest
    fun setup() {
        Theme.defaultTheme = Theme()
    }

    private class TestButton(val title: String) : BaseButton() {
        init {
            minHeight = 10f
            minWidth = 20f
        }

        fun click() {
            uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_DOWN })
            uiInput(InputEvent<Any>().apply { type = InputEvent.Type.TOUCH_UP })
        }
    }

    private class TestTabs : TabContainer() {
        override fun createTabButton(title: String): BaseButton = TestButton(title)

        fun layout() = onSortChildren()
    }

    private fun TabContainer.page(name: String, minWidth: Float = 0f, minHeight: Float = 0f) =
        control {
            this.name = name
            this.minWidth = minWidth
            this.minHeight = minHeight
        }

    private fun TabContainer.button(index: Int) = getTabButton(index) as TestButton

    @Test
    fun test_children_become_tabs_with_name_titles() {
        val tabs = TestTabs()
        tabs.page("Stats")
        tabs.page("Skills")
        assertEquals(2, tabs.tabCount)
        assertEquals("Stats", tabs.button(0).title)
        assertEquals("Skills", tabs.getTabTitle(1))
        assertEquals(2, tabs.tabBar.childCount)
    }

    @Test
    fun test_wrapped_tab_uses_inner_name() {
        val tabs = TestTabs()
        val inner = tabs.control(modifier = Modifier.padding(2)) { name = "Wrapped" }
        assertSame(inner.outermost, tabs.getTab(0))
        assertEquals("Wrapped", tabs.getTabTitle(0))
    }

    @Test
    fun test_only_current_tab_is_visible() {
        val tabs = TestTabs()
        val a = tabs.page("A")
        val b = tabs.page("B")
        assertTrue(a.visible)
        assertFalse(b.visible)
        assertTrue(tabs.button(0).pressed)
        tabs.currentTab = 1
        assertFalse(a.visible)
        assertTrue(b.visible)
        assertTrue(tabs.button(1).pressed)
        assertFalse(tabs.button(0).pressed)
        assertSame(b, tabs.currentTabControl)
    }

    @Test
    fun test_tab_button_switches_tab_and_emits() {
        val tabs = TestTabs()
        tabs.page("A")
        val b = tabs.page("B")
        val changes = mutableListOf<Int>()
        tabs.onTabChanged += { changes += it }
        tabs.button(1).click()
        assertEquals(1, tabs.currentTab)
        assertTrue(b.visible)
        assertEquals(listOf(1), changes)
    }

    @Test
    fun test_current_tab_clamps_when_tabs_are_removed() {
        val tabs = TestTabs()
        tabs.page("A")
        val b = tabs.page("B")
        tabs.currentTab = 5
        assertEquals(1, tabs.currentTab)
        tabs.removeChild(b)
        assertEquals(0, tabs.currentTab)
        assertEquals(1, tabs.tabCount)
        assertEquals(1, tabs.tabBar.childCount)
    }

    @Test
    fun test_set_tab_title() {
        val tabs = TestTabs()
        tabs.page("A")
        tabs.setTabTitle(0, "Inventory")
        assertEquals("Inventory", tabs.getTabTitle(0))
        assertEquals("Inventory", tabs.button(0).title)
    }

    @Test
    fun test_min_size_and_layout() {
        val tabs = TestTabs()
        tabs.page("A", 30f, 40f)
        val b = tabs.page("B", 50f, 60f)
        assertEquals(40f, tabs.combinedMinWidth)
        assertEquals(50f, tabs.combinedMinHeight)
        tabs.useHiddenTabsForMinSize = true
        assertEquals(50f, tabs.combinedMinWidth)
        assertEquals(70f, tabs.combinedMinHeight)
        tabs.size(100f, 100f)
        tabs.layout()
        assertEquals(listOf(0f, 90f, 100f, 10f), tabs.tabBar.rect())
        assertEquals(listOf(0f, 0f, 100f, 90f), b.rect())
    }

    @Test
    fun test_tab_container_dsl() {
        val parent = Node()
        val tabs = parent.tabContainer()
        assertSame(parent, tabs.parent)
        assertEquals(0, tabs.tabCount)
        assertSame(tabs.tabBar, tabs.children[0])
    }

    private fun Control.rect() = listOf(x, y, width, height)
}
