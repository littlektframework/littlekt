package com.littlekt.input

import kotlin.test.Test
import kotlin.test.assertEquals

class PointerTypeTests {

    private class TypedProcessor : InputProcessor {
        val events = mutableListOf<String>()

        override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer, type: PointerType): Boolean {
            events += "down $pointer $type"
            return false
        }

        override fun touchDragged(
            screenX: Float,
            screenY: Float,
            pointer: Pointer,
            type: PointerType,
        ): Boolean {
            events += "drag $pointer $type"
            return false
        }

        override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer, type: PointerType): Boolean {
            events += "up $pointer $type"
            return false
        }

        override fun mouseMoved(screenX: Float, screenY: Float, type: PointerType): Boolean {
            events += "move $type"
            return false
        }
    }

    private class UntypedProcessor : InputProcessor {
        val events = mutableListOf<String>()

        override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
            events += "down $pointer"
            return false
        }

        override fun mouseMoved(screenX: Float, screenY: Float): Boolean {
            events += "move"
            return false
        }
    }

    @Test
    fun test_processors_get_the_pointer_type_of_each_event() {
        val cache = InputCache()
        val processor = TypedProcessor()
        cache.onMove(1f, 1f, Pointer.POINTER1, PointerType.PEN)
        cache.onTouchDown(1f, 1f, Pointer.POINTER2, PointerType.TOUCH)
        cache.onMove(2f, 2f, Pointer.POINTER2, PointerType.TOUCH)
        cache.onTouchUp(2f, 2f, Pointer.POINTER2, PointerType.TOUCH)
        cache.onTouchDown(3f, 3f, Pointer.POINTER1)
        cache.processEvents(listOf(processor))
        assertEquals(
            listOf(
                "move PEN",
                "down POINTER2 TOUCH",
                "drag POINTER2 TOUCH",
                "up POINTER2 TOUCH",
                "down POINTER1 MOUSE",
            ),
            processor.events,
        )
    }

    @Test
    fun test_processors_without_the_type_still_get_the_events() {
        val cache = InputCache()
        val processor = UntypedProcessor()
        cache.onMove(1f, 1f, Pointer.POINTER1, PointerType.PEN)
        cache.onTouchDown(1f, 1f, Pointer.POINTER1, PointerType.TOUCH)
        cache.processEvents(listOf(processor))
        assertEquals(listOf("move", "down POINTER1"), processor.events)
    }

    @Test
    fun test_cache_keeps_the_last_type_of_each_pointer() {
        val cache = InputCache()
        assertEquals(PointerType.MOUSE, cache.getPointerType(Pointer.POINTER3))
        cache.onTouchDown(0f, 0f, Pointer.POINTER3, PointerType.TOUCH)
        cache.onMove(0f, 0f, Pointer.POINTER1, PointerType.PEN)
        assertEquals(PointerType.TOUCH, cache.getPointerType(Pointer.POINTER3))
        assertEquals(PointerType.PEN, cache.getPointerType(Pointer.POINTER1))
    }

    @Test
    fun test_input_defaults_to_mouse() {
        assertEquals(PointerType.MOUSE, FakeInput().getPointerType(Pointer.POINTER1))
    }
}
