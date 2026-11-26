package com.littlekt.graphics

import kotlin.test.Test
import kotlin.test.assertEquals

class TextureInvalidationTests {

    @Test
    fun test_listeners_get_invalidated_ids_until_removed() {
        val ids = mutableListOf<Int>()
        val listener = TextureInvalidationListener { ids += it }
        Texture.addInvalidationListener(listener)
        Texture.notifyInvalidated(-42)
        Texture.removeInvalidationListener(listener)
        Texture.notifyInvalidated(-43)
        assertEquals(listOf(-42), ids)
    }

    @Test
    fun test_removing_one_listener_keeps_the_others() {
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()
        val a = TextureInvalidationListener { first += it }
        val b = TextureInvalidationListener { second += it }
        Texture.addInvalidationListener(a)
        Texture.addInvalidationListener(b)
        Texture.removeInvalidationListener(a)
        Texture.notifyInvalidated(-7)
        Texture.removeInvalidationListener(b)
        assertEquals(emptyList(), first)
        assertEquals(listOf(-7), second)
    }
}
