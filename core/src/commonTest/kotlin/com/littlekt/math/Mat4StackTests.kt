package com.littlekt.math

import com.littlekt.math.geom.degrees
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class Mat4StackTests {

    @Test
    fun test_starts_as_identity() {
        assertContentEquals(Mat4().data, Mat4Stack().data)
    }

    @Test
    fun test_push_pop_restores_matrix() {
        val stack = Mat4Stack()
        stack.setToTranslate(1f, 2f, 3f)
        val saved = Mat4().set(stack)
        stack.push()
        stack.rotate(Vec3f.Z_AXIS, 45.degrees).scale(2f)
        stack.pop()
        assertContentEquals(saved.data, stack.data)
    }

    @Test
    fun test_push_keeps_current_matrix() {
        val stack = Mat4Stack()
        stack.setToScaling(2f, 3f, 4f)
        val saved = Mat4().set(stack)
        stack.push()
        assertContentEquals(saved.data, stack.data)
    }

    @Test
    fun test_nested_push_pop_is_lifo() {
        val stack = Mat4Stack()
        stack.setToTranslate(1f, 0f, 0f)
        stack.push()
        val first = Mat4().set(stack)
        stack.translate(0f, 1f, 0f)
        stack.push()
        val second = Mat4().set(stack)
        stack.translate(0f, 0f, 1f)
        stack.pop()
        assertContentEquals(second.data, stack.data)
        stack.pop()
        assertContentEquals(first.data, stack.data)
    }

    @Test
    fun test_push_pop_return_this() {
        val stack = Mat4Stack()
        assertSame(stack, stack.push())
        assertSame(stack, stack.pop())
        assertSame(stack, stack.reset())
    }

    @Test
    fun test_pop_underflow_throws() {
        assertFailsWith<IllegalStateException> { Mat4Stack().pop() }
    }

    @Test
    fun test_push_overflow_throws() {
        val stack = Mat4Stack(2)
        stack.push().push()
        assertFailsWith<IllegalStateException> { stack.push() }
    }

    @Test
    fun test_full_stack_round_trip() {
        val stack = Mat4Stack(3)
        repeat(3) {
            stack.translate(1f, 0f, 0f)
            stack.push()
        }
        repeat(3) { stack.pop() }
        assertContentEquals(Mat4().setToTranslate(1f, 0f, 0f).data, stack.data)
    }

    @Test
    fun test_reset_clears_stack_and_sets_identity() {
        val stack = Mat4Stack()
        stack.setToTranslate(1f, 2f, 3f)
        stack.push()
        stack.reset()
        assertContentEquals(Mat4().data, stack.data)
        assertFailsWith<IllegalStateException> { stack.pop() }
    }
}
