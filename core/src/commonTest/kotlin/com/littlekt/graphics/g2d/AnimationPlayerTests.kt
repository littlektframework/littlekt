package com.littlekt.graphics.g2d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class AnimationPlayerTests {

    private val frameTime = 100.milliseconds

    private fun animation(vararg frames: String) =
        Animation(frames.toList(), List(frames.size) { it }, List(frames.size) { frameTime })

    private fun AnimationPlayer<String>.step(times: Int = 1) = repeat(times) { update(frameTime) }

    @Test
    fun test_play_once_advances_and_stops() {
        val player = AnimationPlayer<String>()
        val anim = animation("a", "b", "c")
        val changes = mutableListOf<Int>()
        player.onFrameChange = { changes += it }
        player.playOnce(anim)
        assertEquals("a", player.currentKeyFrame)
        player.step()
        assertEquals("b", player.currentKeyFrame)
        player.step()
        assertEquals("c", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
        assertEquals(listOf(0, 1, 2), changes)
    }

    @Test
    fun test_play_times_repeats() {
        val player = AnimationPlayer<String>()
        player.play(animation("a", "b"), times = 2)
        player.step(3)
        assertEquals("b", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_play_looped_wraps() {
        val player = AnimationPlayer<String>()
        val anim = animation("a", "b")
        player.playLooped(anim)
        player.step(3)
        assertEquals("b", player.currentKeyFrame)
        player.step(97)
        assertEquals(anim, player.currentAnimation)
        assertEquals("a", player.currentKeyFrame)
    }

    @Test
    fun test_total_frames_played() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b"))
        player.step(5)
        assertEquals(5, player.totalFramesPlayed)
        assertEquals(2, player.totalFrames)
    }

    @Test
    fun test_queued_animation_starts_at_first_frame() {
        val player = AnimationPlayer<String>()
        val first = animation("a0", "a1")
        val second = animation("b0", "b1")
        player.play(first)
        player.play(second, queue = true)
        assertEquals(first, player.currentAnimation)
        player.step(2)
        assertEquals(second, player.currentAnimation)
        assertEquals("b0", player.currentKeyFrame)
        player.step()
        assertEquals("b1", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_play_replaces_current_without_queue() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b"))
        player.step()
        val next = animation("c", "d")
        player.play(next)
        assertEquals(next, player.currentAnimation)
        assertEquals("c", player.currentKeyFrame)
    }

    @Test
    fun test_new_animation_resets_frame_timer() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b"))
        player.update(40.milliseconds)
        player.playLooped(animation("c", "d"))
        player.update(40.milliseconds)
        assertEquals("c", player.currentKeyFrame)
        player.update(40.milliseconds)
        assertEquals("c", player.currentKeyFrame)
        player.update(20.milliseconds)
        assertEquals("d", player.currentKeyFrame)
    }

    @Test
    fun test_half_frame_dt_needs_two_updates_per_frame() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b", "c"))
        player.update(50.milliseconds)
        assertEquals("a", player.currentKeyFrame)
        player.update(50.milliseconds)
        assertEquals("b", player.currentKeyFrame)
        player.update(50.milliseconds)
        assertEquals("b", player.currentKeyFrame)
        player.update(50.milliseconds)
        assertEquals("c", player.currentKeyFrame)
        assertEquals(2, player.totalFramesPlayed)
    }

    @Test
    fun test_large_dt_advances_multiple_frames_and_carries_leftover() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b", "c", "d"))
        player.update(250.milliseconds)
        assertEquals("c", player.currentKeyFrame)
        assertEquals(2, player.totalFramesPlayed)
        player.update(50.milliseconds)
        assertEquals("d", player.currentKeyFrame)
        assertEquals(3, player.totalFramesPlayed)
    }

    @Test
    fun test_leftover_time_keeps_playback_in_sync() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b"))
        repeat(10) { player.update(70.milliseconds) }
        assertEquals(7, player.totalFramesPlayed)
        assertEquals("b", player.currentKeyFrame)
    }

    @Test
    fun test_zero_duration_frames_are_skipped() {
        val player = AnimationPlayer<String>()
        val times = listOf(frameTime, Duration.ZERO, frameTime)
        player.playLooped(Animation(listOf("a", "b", "c"), listOf(0, 1, 2), times))
        player.update(frameTime)
        assertEquals("c", player.currentKeyFrame)
        assertEquals(2, player.totalFramesPlayed)
    }

    @Test
    fun test_all_zero_duration_looped_frames_do_not_hang() {
        val player = AnimationPlayer<String>()
        val anim = Animation(listOf("a", "b"), listOf(0, 1), listOf(Duration.ZERO, Duration.ZERO))
        player.playLooped(anim)
        player.update(16.milliseconds)
        assertEquals(anim, player.currentAnimation)
        assertEquals(2, player.totalFramesPlayed)
    }

    @Test
    fun test_all_zero_duration_once_finishes_in_one_update() {
        val player = AnimationPlayer<String>()
        player.playOnce(Animation(listOf("a", "b"), listOf(0, 1), listOf(Duration.ZERO, Duration.ZERO)))
        player.update(16.milliseconds)
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_empty_animation_finishes_without_crashing() {
        val player = AnimationPlayer<String>()
        val empty = Animation<String>(emptyList(), emptyList(), emptyList())
        player.playOnce(empty)
        player.update(frameTime)
        assertNull(player.currentKeyFrame)
        player.playLooped(empty)
        player.update(frameTime)
        assertNull(player.currentKeyFrame)
    }

    @Test
    fun test_looped_huge_dt_skips_whole_cycles() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b", "c"))
        player.update(1.hours + frameTime)
        assertEquals("b", player.currentKeyFrame)
        assertEquals(36001, player.totalFramesPlayed)
    }

    @Test
    fun test_looped_infinite_dt_does_not_hang() {
        val player = AnimationPlayer<String>()
        val anim = animation("a", "b")
        player.playLooped(anim)
        player.update(Duration.INFINITE)
        assertEquals(anim, player.currentAnimation)
        player.step()
        assertEquals("b", player.currentKeyFrame)
    }

    @Test
    fun test_play_for_duration_with_large_dt() {
        val player = AnimationPlayer<String>()
        player.play(animation("a", "b"), 250.milliseconds)
        player.update(200.milliseconds)
        assertEquals("a", player.currentKeyFrame)
        player.update(100.milliseconds)
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_queue_for_duration_leaves_current_animation_running() {
        val player = AnimationPlayer<String>()
        val first = animation("a0", "a1", "a2")
        val second = animation("b0", "b1")
        player.play(first)
        player.step()
        player.play(second, 150.milliseconds, queue = true)
        assertEquals(first, player.currentAnimation)
        assertEquals("a1", player.currentKeyFrame)
        player.step()
        assertEquals("a2", player.currentKeyFrame)
        player.step()
        assertEquals(second, player.currentAnimation)
        assertEquals("b0", player.currentKeyFrame)
        player.step(2)
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_queue_on_idle_player_starts_playing() {
        val player = AnimationPlayer<String>()
        player.play(animation("a", "b"), queue = true)
        player.step()
        assertEquals("b", player.currentKeyFrame)
    }

    @Test
    fun test_queue_for_duration_on_idle_player_starts_playing() {
        val player = AnimationPlayer<String>()
        player.play(animation("a", "b"), 150.milliseconds, queue = true)
        player.step()
        assertEquals("b", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_play_single_frame_updates_duration() {
        val player = AnimationPlayer<String>()
        player.play("x", 50.milliseconds, numFrames = 3)
        assertEquals(150.milliseconds, player.currentAnimation?.duration)
        player.play("y", 20.milliseconds, numFrames = 2)
        assertEquals(40.milliseconds, player.currentAnimation?.duration)
    }

    @Test
    fun test_play_single_frame_for_num_frames() {
        val player = AnimationPlayer<String>()
        player.play("x", frameTime, numFrames = 3)
        assertEquals(3, player.totalFrames)
        player.step(2)
        assertEquals("x", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_play_for_duration() {
        val player = AnimationPlayer<String>()
        player.play(animation("a", "b"), 250.milliseconds)
        player.step(2)
        assertEquals("a", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_stop_and_resume() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b", "c"))
        player.step()
        player.stop()
        player.step(5)
        assertEquals("b", player.currentKeyFrame)
        player.resume()
        player.step()
        assertEquals("c", player.currentKeyFrame)
    }

    @Test
    fun test_restart_replays_full_animation() {
        val player = AnimationPlayer<String>()
        val anim = animation("a", "b", "c", "d")
        player.playOnce(anim)
        player.step(2)
        player.restart()
        assertEquals("a", player.currentKeyFrame)
        player.step(3)
        assertEquals("d", player.currentKeyFrame)
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_start_after_stop_begins_at_first_frame() {
        val player = AnimationPlayer<String>()
        player.playLooped(animation("a", "b", "c"))
        player.step(2)
        player.stop()
        player.start()
        assertEquals("a", player.currentKeyFrame)
        player.step()
        assertEquals("b", player.currentKeyFrame)
    }

    @Test
    fun test_state_with_highest_priority_plays() {
        val player = AnimationPlayer<String>()
        val idle = animation("idle")
        val run = animation("run")
        var running = false
        player.registerState(idle, 0)
        player.registerState(run, 1) { running }
        player.step()
        assertEquals(idle, player.currentAnimation)
        running = true
        player.step()
        assertEquals(run, player.currentAnimation)
        running = false
        player.step()
        assertEquals(idle, player.currentAnimation)
    }

    @Test
    fun test_manual_play_overrides_states_until_done() {
        val player = AnimationPlayer<String>()
        val idle = animation("idle")
        val attack = animation("a0", "a1")
        player.registerState(idle, 0)
        player.step()
        player.playOnce(attack)
        player.step()
        assertEquals(attack, player.currentAnimation)
        player.step()
        assertNull(player.currentAnimation)
        player.step()
        assertEquals(idle, player.currentAnimation)
    }

    @Test
    fun test_remove_state() {
        val player = AnimationPlayer<String>()
        val idle = animation("idle")
        val run = animation("run")
        player.registerState(idle, 0)
        player.registerState(run, 1)
        player.removeState(run)
        player.step()
        assertEquals(idle, player.currentAnimation)
        player.removeAllStates()
        player.stop()
        player.playOnce(animation("x"))
        player.step()
        assertNull(player.currentAnimation)
    }

    @Test
    fun test_register_state_replaces_existing() {
        val player = AnimationPlayer<String>()
        val idle = animation("idle")
        val run = animation("run")
        player.registerState(run, 5)
        player.registerState(idle, 1)
        player.registerState(run, 0)
        player.step()
        assertEquals(idle, player.currentAnimation)
    }

    @Test
    fun test_no_animation_defaults() {
        val player = AnimationPlayer<String>()
        assertNull(player.currentAnimation)
        assertNull(player.currentKeyFrame)
        assertEquals(1, player.totalFrames)
        player.step()
        player.start()
        assertEquals(0, player.totalFramesPlayed)
    }
}
