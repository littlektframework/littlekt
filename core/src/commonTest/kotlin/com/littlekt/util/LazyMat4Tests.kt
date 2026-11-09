package com.littlekt.util

import com.littlekt.math.Mat4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class LazyMat4Tests {

    @Test
    fun test_get_updates_once_while_clean() {
        var calls = 0
        val lazy = LazyMat4 {
            calls++
            it.setToTranslate(1f, 2f, 3f)
        }
        val a = lazy.get()
        val b = lazy.get()
        assertEquals(1, calls)
        assertSame(a, b)
        assertEquals(Mat4().setToTranslate(1f, 2f, 3f), a)
        assertFalse(lazy.isDirty)
    }

    @Test
    fun test_dirty_triggers_update() {
        var calls = 0
        val lazy = LazyMat4 { calls++ }
        lazy.get()
        lazy.isDirty = true
        lazy.get()
        assertEquals(2, calls)
    }

    @Test
    fun test_clear_resets_to_identity_without_update() {
        var calls = 0
        val lazy = LazyMat4 {
            calls++
            it.setToTranslate(1f, 2f, 3f)
        }
        lazy.get()
        lazy.clear()
        assertEquals(Mat4(), lazy.get())
        assertEquals(1, calls)
    }
}
