package com.littlekt.graph.node.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OptionButtonTests {

    init {
        useTestTheme()
    }

    @Test
    fun test_selected_sets_text_and_item() {
        val option = createOptionButton(listOf("Grass", "Sand", "Stone"), 1)
        assertEquals(1, option.selected)
        assertEquals("Sand", option.text)
        assertEquals("Sand", option.selectedItem)
    }

    @Test
    fun test_out_of_range_selection_clears_to_placeholder() {
        val option = createOptionButton(listOf("Grass"), 0)
        option.placeholder = "Pick one"
        option.selected = 5
        assertEquals(-1, option.selected)
        assertEquals("Pick one", option.text)
        assertNull(option.selectedItem)
    }

    @Test
    fun test_first_added_item_is_selected_without_placeholder() {
        val option = OptionButton()
        option.addItem("Grass")
        option.addItem("Sand")
        assertEquals(0, option.selected)
        assertEquals("Grass", option.text)
    }

    @Test
    fun test_set_items_keeps_selection_in_range() {
        val option = createOptionButton(listOf("a", "b", "c"), 2)
        option.setItems(listOf("x", "y", "z", "w"))
        assertEquals("z", option.text)
        option.setItems(listOf("x"))
        assertEquals(-1, option.selected)
        option.clearItems()
        assertEquals(0, option.items.size)
    }

    @Test
    fun test_open_list_needs_a_scene() {
        val option = createOptionButton(listOf("a", "b"), 0)
        option.openList()
        assertNull(option.popup)
    }
}
