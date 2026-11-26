package com.littlekt.audio

import com.littlekt.file.UnsupportedFileTypeException
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AudioSamplesJvmTests {

    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/audio/$name")) { "Missing $name" }.use { it.readBytes() }

    private fun AudioSamples.rms(channel: Int): Float {
        var sum = 0.0
        for (f in 0 until frames) sum += this[f, channel] * this[f, channel]
        return sqrt(sum / frames).toFloat()
    }

    private fun AudioSamples.frequency(channel: Int): Float {
        val start = frames / 4
        val end = frames * 3 / 4
        var crossings = 0
        for (f in start + 1 until end) {
            if (this[f - 1, channel] < 0f && this[f, channel] >= 0f) crossings++
        }
        return crossings * sampleRate / (end - start).toFloat()
    }

    private fun assertTone(samples: AudioSamples) {
        for (c in 0 until samples.channels) {
            assertTrue(samples.rms(c) > 0.05f, "Channel $c is silent")
            assertTrue(abs(samples.frequency(c) - 440f) < 25f, "Channel $c is ${samples.frequency(c)} Hz")
        }
        assertTrue(samples.samples.all { it in -1f..1f })
    }

    @Test
    fun test_decodes_ogg_vorbis() = runTest {
        val samples = resource("tone_mono_22050.ogg").decodeAudioSamples()
        assertEquals(1, samples.channels)
        assertEquals(22050, samples.sampleRate)
        assertEquals(11025, samples.frames)
        assertTone(samples)
    }

    @Test
    fun test_decodes_mp3() = runTest {
        val samples = resource("tone_stereo_44100.mp3").decodeAudioSamples()
        assertEquals(2, samples.channels)
        assertEquals(44100, samples.sampleRate)
        assertTrue(samples.frames in 22050..26000, "Decoded ${samples.frames} frames")
        assertTone(samples)
    }

    @Test
    fun test_rejects_unknown_formats() = runTest {
        assertFailsWith<UnsupportedFileTypeException> { "fLaC".encodeToByteArray().decodeAudioSamples() }
    }
}
