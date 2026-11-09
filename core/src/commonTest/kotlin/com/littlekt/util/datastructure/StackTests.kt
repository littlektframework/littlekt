package com.littlekt.util.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTests {

    @Test
    fun test_push_pop_is_lifo() {
        val stack = Stack<String>()
        stack.push("a")
        stack.push("b")
        assertEquals("b", stack.pop())
        assertEquals("a", stack.pop())
        assertTrue(stack.isEmpty())
    }

    @Test
    fun test_peek_does_not_remove() {
        val stack = Stack("a", "b")
        assertEquals("b", stack.peek())
        assertEquals(2, stack.size)
    }

    @Test
    fun test_peek_empty_is_null() {
        assertNull(Stack<String>().peek())
        assertNull(IntStack().peek())
        assertNull(FloatStack().peek())
        assertNull(DoubleStack().peek())
    }

    @Test
    fun test_pop_empty_throws() {
        assertFailsWith<IndexOutOfBoundsException> { Stack<String>().pop() }
        assertFailsWith<IndexOutOfBoundsException> { IntStack().pop() }
    }

    @Test
    fun test_contains_and_iteration_order() {
        val stack = Stack(1, 2, 3)
        assertTrue(stack.contains(2))
        assertFalse(stack.contains(4))
        assertTrue(stack.containsAll(listOf(1, 3)))
        assertEquals(listOf(1, 2, 3), stack.toList())
    }

    @Test
    fun test_equals() {
        assertEquals(Stack(1, 2), Stack(1, 2))
        assertNotEquals(Stack(1, 2), Stack(2, 1))
        assertEquals(IntStack(1, 2), IntStack(1, 2))
        assertEquals(IntStack(1, 2).hashCode(), IntStack(1, 2).hashCode())
    }

    @Test
    fun test_int_stack() {
        val stack = IntStack(1, 2)
        stack.push(3)
        assertEquals(3, stack.peek())
        assertEquals(3, stack.pop())
        assertEquals(2, stack.pop())
        assertEquals(1, stack.size)
        assertTrue(stack.contains(1))
    }

    @Test
    fun test_int_stack_reuse_after_drain() {
        val stack = IntStack()
        repeat(10) { stack.push(it) }
        repeat(10) { stack.pop() }
        stack.push(42)
        assertEquals(1, stack.size)
        assertEquals(42, stack.peek())
    }

    @Test
    fun test_float_stack() {
        val stack = FloatStack(1f)
        stack.push(2f)
        assertEquals(2f, stack.pop())
        assertEquals(1f, stack.peek())
        assertEquals(listOf(1f), stack.toList())
    }

    @Test
    fun test_double_stack() {
        val stack = DoubleStack(1.0, 2.0)
        assertEquals(2.0, stack.pop())
        assertFalse(stack.contains(2.0))
        assertTrue(stack.containsAll(listOf(1.0)))
    }
}
