package com.littlekt.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class PcmFillerTests {

    @Test
    fun test_fills_interleaved_frames_and_counts_them() {
        var calls = 0
        var next = 0f
        val filler =
            PcmFiller(48000, 2, 4, 3) { buffer, frames ->
                calls++
                assertEquals(4, frames)
                assertEquals(8, buffer.size)
                for (i in 0 until frames * 2) buffer[i] = next++
            }
        val first = filler.fill()
        assertContentEquals(floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f), first)
        assertEquals(4L, filler.framesWritten)
        filler.fill()
        assertEquals(2, calls)
        assertEquals(8L, filler.framesWritten)
    }

    @Test
    fun test_reuses_and_zeroes_the_buffer() {
        var write = true
        val filler = PcmFiller(44100, 1, 3, 2) { buffer, _ -> if (write) buffer.fill(1f) }
        val first = filler.fill()
        write = false
        val second = filler.fill()
        assertSame(first, second)
        assertContentEquals(floatArrayOf(0f, 0f, 0f), second)
    }

    @Test
    fun test_latency_covers_every_buffer() {
        val filler = PcmFiller(48000, 2, 480, 4) { _, _ -> }
        assertEquals(40.milliseconds, filler.latency)
    }

    @Test
    fun test_rejects_invalid_settings() {
        val source = PcmSource { _, _ -> }
        assertFailsWith<IllegalArgumentException> { PcmFiller(0, 2, 256, 2, source) }
        assertFailsWith<IllegalArgumentException> { PcmFiller(44100, 3, 256, 2, source) }
        assertFailsWith<IllegalArgumentException> { PcmFiller(44100, 2, 0, 2, source) }
        assertFailsWith<IllegalArgumentException> { PcmFiller(44100, 2, 256, 1, source) }
    }
}
