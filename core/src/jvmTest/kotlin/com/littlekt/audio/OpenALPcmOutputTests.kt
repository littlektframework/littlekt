package com.littlekt.audio

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger

class OpenALPcmOutputTests {
    private lateinit var context: OpenALAudioContext

    @BeforeTest
    fun setup() {
        context = OpenALAudioContext(maxSources = 4)
    }

    @AfterTest
    fun tearDown() {
        context.release()
    }

    private fun waitFor(timeoutMillis: Long = 3000, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            if (condition()) return true
            Thread.sleep(5)
        }
        return condition()
    }

    @Test
    fun test_starts_paused_without_filling() {
        val calls = AtomicInteger()
        val output = context.createPcmOutput(bufferFrames = 256, bufferCount = 3) { _, _ -> calls.incrementAndGet() }
        Thread.sleep(50)
        assertFalse(output.playing)
        assertEquals(0, calls.get())
        assertEquals(0L, output.framesWritten)
        output.release()
    }

    @Test
    fun test_fills_every_buffer_on_the_audio_thread() {
        if (context.NO_DEVICE) return
        val threads = mutableSetOf<String>()
        val output =
            context.createPcmOutput(sampleRate = 48000, channels = 1, bufferFrames = 256, bufferCount = 3) { _, frames ->
                assertEquals(256, frames)
                synchronized(threads) { threads += Thread.currentThread().name }
            }
        output.play()
        assertTrue(output.playing)
        assertTrue(waitFor { output.framesWritten >= 256L * 3 })
        assertTrue(waitFor { output.framesWritten >= 256L * 6 }, "Buffers weren't refilled")
        assertEquals(0L, output.framesWritten % 256)
        assertEquals(setOf("LittleKt PCM Audio"), synchronized(threads) { threads.toSet() })
        output.release()
    }

    @Test
    fun test_pause_stops_filling() {
        if (context.NO_DEVICE) return
        val output = context.createPcmOutput(bufferFrames = 256, bufferCount = 2) { _, _ -> }
        output.play()
        assertTrue(waitFor { output.framesWritten >= 512L })
        output.pause()
        assertFalse(output.playing)
        Thread.sleep(30)
        val paused = output.framesWritten
        Thread.sleep(100)
        assertEquals(paused, output.framesWritten)
        output.play()
        assertTrue(waitFor { output.framesWritten > paused })
        output.release()
    }

    @Test
    fun test_stop_frees_the_reserved_source() {
        if (context.NO_DEVICE) return
        val outputs = List(4) { context.createPcmOutput(bufferFrames = 256) { _, _ -> } }
        outputs.forEach { it.play() }
        assertEquals(-1, context.obtainSource())
        outputs[0].stop()
        assertFalse(outputs[0].playing)
        assertTrue(context.obtainSource() != -1)
        outputs.forEach { it.release() }
    }

    @Test
    fun test_a_failing_source_pauses_its_output() {
        if (context.NO_DEVICE) return
        val output = context.createPcmOutput(bufferFrames = 256) { _, _ -> error("boom") }
        output.play()
        assertTrue(waitFor { !output.playing })
        output.release()
    }

    @Test
    fun test_context_release_releases_outputs() {
        if (context.NO_DEVICE) return
        val output = context.createPcmOutput(bufferFrames = 256) { _, _ -> }
        output.play()
        assertTrue(waitFor { output.framesWritten > 0L })
        context.release()
        assertFalse(output.playing)
        val written = output.framesWritten
        Thread.sleep(50)
        assertEquals(written, output.framesWritten)
        context = OpenALAudioContext(maxSources = 4)
    }
}
