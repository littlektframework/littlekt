package com.littlekt.audio

import java.nio.ShortBuffer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10

class OpenALAudioContextTests {
    private lateinit var context: OpenALAudioContext
    private var buffer = 0

    @BeforeTest
    fun setup() {
        context = OpenALAudioContext(maxSources = 4)
        if (context.NO_DEVICE) return
        buffer = AL10.alGenBuffers()
        val silence: ShortBuffer = BufferUtils.createShortBuffer(4410)
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, silence, 44100)
    }

    @AfterTest
    fun tearDown() {
        if (!context.NO_DEVICE) AL10.alDeleteBuffers(buffer)
        context.release()
    }

    private fun playLooping(sourceId: Int) {
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, buffer)
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE)
        AL10.alSourcePlay(sourceId)
    }

    @Test
    fun test_creates_sources_up_to_the_maximum() {
        if (context.NO_DEVICE) return
        val ids = List(4) { context.obtainSource().also(::playLooping) }
        assertEquals(4, ids.toSet().size)
    }

    @Test
    fun test_reuses_idle_sources_first() {
        if (context.NO_DEVICE) return
        val first = context.obtainSource()
        assertEquals(first, context.obtainSource())
    }

    @Test
    fun test_takes_the_oldest_playing_clip_when_full() {
        if (context.NO_DEVICE) return
        val ids = List(4) { context.obtainSource().also(::playLooping) }
        assertEquals(ids[0], context.obtainSource().also(::playLooping))
        assertEquals(ids[1], context.obtainSource())
    }

    @Test
    fun test_reserved_sources_are_never_taken() {
        if (context.NO_DEVICE) return
        val stream = context.obtainSource(reserve = true).also(::playLooping)
        val clips = List(3) { context.obtainSource().also(::playLooping) }
        repeat(6) { assertNotEquals(stream, context.obtainSource().also(::playLooping)) }
        context.releaseSource(stream)
        AL10.alSourceStop(stream)
        assertTrue(stream in clips + stream)
        assertEquals(stream, context.obtainSource())
    }

    @Test
    fun test_all_reserved_returns_no_source() {
        if (context.NO_DEVICE) return
        repeat(4) { context.obtainSource(reserve = true).also(::playLooping) }
        assertEquals(-1, context.obtainSource())
    }
}
