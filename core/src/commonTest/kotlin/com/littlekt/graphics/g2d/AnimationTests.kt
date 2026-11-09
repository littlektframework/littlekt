package com.littlekt.graphics.g2d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AnimationTests {

    private fun animation(vararg times: Int) =
        Animation(
            List(times.size) { "f$it" },
            List(times.size) { it },
            times.map { it.milliseconds },
        )

    @Test
    fun test_sizes_and_duration() {
        val anim = animation(100, 200, 50)
        assertEquals(3, anim.frameStackSize)
        assertEquals(3, anim.totalFrames)
        assertEquals("f0", anim.firstFrame)
        assertEquals("f1", anim[1])
        assertEquals(350.milliseconds, anim.duration)
    }

    @Test
    fun test_empty_duration_is_zero() {
        assertEquals(Duration.ZERO, Animation<String>(emptyList(), emptyList(), emptyList()).duration)
    }

    @Test
    fun test_get_frame_by_time() {
        val anim = animation(100, 100, 100)
        assertEquals("f0", anim.getFrame(Duration.ZERO))
        assertEquals("f0", anim.getFrame(100.milliseconds))
        assertEquals("f1", anim.getFrame(150.milliseconds))
        assertEquals("f2", anim.getFrame(250.milliseconds))
        assertEquals("f2", anim.getFrame(300.milliseconds))
    }

    @Test
    fun test_get_frame_by_time_past_duration_clamps_to_last() {
        val anim = animation(100, 100, 100)
        assertEquals("f2", anim.getFrame(301.milliseconds))
        assertEquals("f2", anim.getFrame(10_000.milliseconds))
    }

    @Test
    fun test_get_frame_by_time_uneven_frames() {
        val anim = animation(50, 200, 50)
        assertEquals("f1", anim.getFrame(60.milliseconds))
        assertEquals("f1", anim.getFrame(250.milliseconds))
        assertEquals("f2", anim.getFrame(251.milliseconds))
    }

    @Test
    fun test_get_frame_by_index_follows_indices_and_wraps() {
        val anim =
            Animation(listOf("a", "b", "c"), listOf(2, 0, 1), List(3) { 100.milliseconds })
        assertEquals("c", anim.getFrame(0))
        assertEquals("a", anim.getFrame(1))
        assertEquals("b", anim.getFrame(2))
        assertEquals("c", anim.getFrame(3))
        assertEquals("b", anim.getFrame(-1))
    }

    @Test
    fun test_get_frame_time_uses_play_order() {
        val anim =
            Animation(
                listOf("a", "b", "c", "d"),
                listOf(3, 0),
                listOf(500.milliseconds, 100.milliseconds),
            )
        assertEquals(500.milliseconds, anim.getFrameTime(0))
        assertEquals(100.milliseconds, anim.getFrameTime(1))
        assertEquals(500.milliseconds, anim.getFrameTime(2))
    }

    @Test
    fun test_equality_is_by_instance() {
        val a = animation(100)
        val b = animation(100)
        assertEquals(a, a)
        assertNotEquals(a, b)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun test_builder_ranges_and_repeats() {
        val anim =
            AnimationBuilder(listOf("a", "b", "c", "d"))
                .apply {
                    frames(indices = 1..2, repeats = 1, frameTime = 50.milliseconds)
                    frames(index = 0, frameTime = 200.milliseconds)
                }
                .build()
        assertEquals(listOf(1, 2, 1, 2, 0), anim.frameIndices)
        assertEquals(5, anim.totalFrames)
        assertEquals(400.milliseconds, anim.duration)
        assertEquals("a", anim.getFrame(4))
        assertEquals(200.milliseconds, anim.getFrameTime(4))
        assertEquals(50.milliseconds, anim.getFrameTime(3))
    }

    @Test
    fun test_builder_single_frame_repeats() {
        val anim =
            AnimationBuilder(listOf("a", "b"))
                .apply { frames(index = 1, repeats = 2, frameTime = 10.milliseconds) }
                .build()
        assertEquals(listOf(1, 1, 1), anim.frameIndices)
        assertEquals(30.milliseconds, anim.duration)
    }

    @Test
    fun test_builder_time_lookup_matches_frame_order() {
        val anim =
            AnimationBuilder(listOf("a", "b", "c"))
                .apply {
                    frames(index = 2, frameTime = 300.milliseconds)
                    frames(index = 0, frameTime = 100.milliseconds)
                }
                .build()
        assertEquals("c", anim.getFrame(299.milliseconds))
        assertEquals("a", anim.getFrame(301.milliseconds))
    }
}
