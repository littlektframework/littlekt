package com.littlekt.graphics.g2d

import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.Extent3D
import com.littlekt.graphics.webgpu.Sampler
import com.littlekt.graphics.webgpu.SamplerDescriptor
import com.littlekt.graphics.webgpu.TextureDescriptor
import com.littlekt.graphics.webgpu.TextureView
import com.littlekt.graphics.webgpu.TextureViewDescriptor
import com.littlekt.graphics.webgpu.WebGPUTexture
import com.littlekt.math.Mat4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpriteDrawCallsTests {

    private class FakeTexture(override val id: Int) : Texture {
        override val size: Extent3D = Extent3D(1, 1, 1)
        override var textureDescriptor: TextureDescriptor
            get() = error("No GPU")
            set(_) = Unit

        override val gpuTexture: WebGPUTexture
            get() = error("No GPU")

        override var textureViewDescriptor: TextureViewDescriptor? = null
        override val view: TextureView
            get() = error("No GPU")

        override var samplerDescriptor: SamplerDescriptor
            get() = error("No GPU")
            set(_) = Unit

        override val sampler: Sampler
            get() = error("No GPU")

        override fun writeDataToBuffer() = Unit

        override fun release() = Unit
    }

    private val texture = FakeTexture(1)
    private val info = Any()
    private val matrix = Mat4()
    private val none = ScissorRect()

    private fun rect(x: Int, y: Int, w: Int, h: Int) = ScissorRect().apply { set(x, y, w, h) }

    private fun SpriteDrawCalls.draw(scissor: ScissorRect, offset: Int, count: Int = 1) {
        next(texture, info, matrix, scissor, offset).instances += count
    }

    @Test
    fun test_scissor_changes_record_ranges_without_new_camera_slots() {
        val calls = SpriteDrawCalls()
        calls.draw(rect(0, 0, 10, 10), 0, 2)
        calls.draw(rect(5, 5, 10, 10), 2, 3)
        calls.draw(rect(0, 0, 10, 10), 5, 1)
        assertEquals(3, calls.size)
        assertEquals(0, calls[0].offset)
        assertEquals(2, calls[0].instances)
        assertEquals(2, calls[1].offset)
        assertEquals(3, calls[1].instances)
        assertEquals(5, calls[2].offset)
        assertTrue(calls[0].matrixChanged)
        assertFalse(calls[1].matrixChanged)
        assertFalse(calls[2].matrixChanged)
        assertTrue(calls[0].scissorChanged)
        assertTrue(calls[1].scissorChanged)
        assertTrue(calls[2].scissorChanged)
        assertEquals(5, calls[1].scissor.x)
    }

    @Test
    fun test_matrix_changes_need_a_new_camera_slot() {
        val calls = SpriteDrawCalls()
        calls.draw(none, 0)
        val moved = Mat4().translate(1f, 0f, 0f)
        calls.next(texture, info, moved, none, 1).instances++
        assertTrue(calls[1].matrixChanged)
    }

    @Test
    fun test_same_scissor_after_a_texture_change_isnt_set_again() {
        val calls = SpriteDrawCalls()
        val clip = rect(1, 2, 3, 4)
        calls.draw(clip, 0)
        calls.next(FakeTexture(2), info, matrix, clip, 1).instances++
        assertTrue(calls[0].scissorChanged)
        assertFalse(calls[1].scissorChanged)
    }

    @Test
    fun test_unclipped_draws_keep_the_pass_scissor() {
        val calls = SpriteDrawCalls()
        val clip = rect(1, 2, 3, 4)
        calls.draw(clip, 0)
        calls.draw(none, 1)
        calls.draw(clip, 2)
        assertFalse(calls[1].scissorChanged)
        assertTrue(calls[1].passScissor.matches(clip))
        assertFalse(calls[2].scissorChanged)
    }

    @Test
    fun test_empty_draw_call_is_reused() {
        val calls = SpriteDrawCalls()
        calls.draw(none, 0)
        calls.next(texture, info, matrix, rect(0, 0, 1, 1), 1)
        calls.draw(rect(0, 0, 2, 2), 1)
        assertEquals(2, calls.size)
        assertEquals(2, calls[1].scissor.width)
        assertTrue(calls[1].scissorChanged)
    }

    @Test
    fun test_clear_starts_a_new_flush() {
        val calls = SpriteDrawCalls()
        val clip = rect(1, 2, 3, 4)
        calls.draw(clip, 0)
        calls.clear()
        calls.draw(clip, 0)
        assertEquals(1, calls.size)
        assertTrue(calls[0].matrixChanged)
        assertTrue(calls[0].scissorChanged)
    }
}
