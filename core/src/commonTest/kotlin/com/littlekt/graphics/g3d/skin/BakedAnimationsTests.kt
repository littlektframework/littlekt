package com.littlekt.graphics.g3d.skin

import com.littlekt.graphics.g3d.MeshPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BakedAnimationsTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun clip(duration: Float, fps: Float = 10f, firstFrame: Int = 0) =
        BakedAnimations.Clip(
            "clip",
            0,
            firstFrame,
            kotlin.math.ceil(duration * fps).toInt() + 1,
            duration,
            fps,
        )

    @Test
    fun test_frame_times_end_at_duration() {
        val clip = clip(0.25f)
        assertEquals(4, clip.frameCount)
        assertNear(0f, clip.frameTime(0))
        assertNear(0.2f, clip.frameTime(2))
        assertNear(0.25f, clip.frameTime(3))
    }

    @Test
    fun test_frame_and_blend_between_samples() {
        val clip = clip(1f)
        assertEquals(3, clip.frameAt(0.35f))
        assertNear(0.5f, clip.blendAt(0.35f), 1e-4f)
        assertEquals(0, clip.frameAt(0f))
        assertNear(0f, clip.blendAt(0f))
    }

    @Test
    fun test_short_last_interval_blends_to_the_end() {
        val clip = clip(0.25f)
        assertEquals(2, clip.frameAt(0.225f))
        assertNear(0.5f, clip.blendAt(0.225f), 1e-4f)
        assertEquals(2, clip.frameAt(0.25f))
        assertNear(1f, clip.blendAt(0.25f))
    }

    @Test
    fun test_time_is_clamped_to_the_clip() {
        val clip = clip(1f)
        assertEquals(clip.frameAt(1f), clip.frameAt(5f))
        assertNear(1f, clip.blendAt(5f))
        assertEquals(0, clip.frameAt(-1f))
    }

    @Test
    fun test_single_frame_clip_never_blends() {
        val clip = BakedAnimations.Clip(null, 0, 0, 1, 0f, 30f)
        assertEquals(0, clip.frameAt(0.5f))
        assertNear(0f, clip.blendAt(0.5f))
    }

    @Test
    fun test_instance_data_has_room_for_skin_frames() {
        assertEquals(24, MeshPrimitive.INSTANCE_DATA_SIZE)
    }
}
