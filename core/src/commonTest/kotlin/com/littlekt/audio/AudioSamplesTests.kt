package com.littlekt.audio

import com.littlekt.file.UnsupportedFileTypeException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AudioSamplesTests {

    private class Chunk(val id: String, val body: ByteArray)

    private fun wav(format: Int, channels: Int, sampleRate: Int, bits: Int, data: ByteArray, vararg extra: Chunk): ByteArray {
        val fmt = ByteArray(16)
        fmt.p16(0, format)
        fmt.p16(2, channels)
        fmt.p32(4, sampleRate)
        fmt.p32(8, sampleRate * channels * bits / 8)
        fmt.p16(12, channels * bits / 8)
        fmt.p16(14, bits)
        val chunks = listOf(Chunk("fmt ", fmt)) + extra + Chunk("data", data)
        val size = 4 + chunks.sumOf { 8 + it.body.size + (it.body.size and 1) }
        val out = ByteArray(8 + size)
        "RIFF".encodeToByteArray().copyInto(out, 0)
        out.p32(4, size)
        "WAVE".encodeToByteArray().copyInto(out, 8)
        var o = 12
        chunks.forEach {
            it.id.encodeToByteArray().copyInto(out, o)
            out.p32(o + 4, it.body.size)
            it.body.copyInto(out, o + 8)
            o += 8 + it.body.size + (it.body.size and 1)
        }
        return out
    }

    private fun ByteArray.p16(o: Int, v: Int) {
        this[o] = v.toByte()
        this[o + 1] = (v shr 8).toByte()
    }

    private fun ByteArray.p32(o: Int, v: Int) {
        p16(o, v)
        p16(o + 2, v shr 16)
    }

    private fun pcm16(vararg samples: Int) = ByteArray(samples.size * 2).apply { samples.forEachIndexed { i, s -> p16(i * 2, s) } }

    private fun cue(vararg frames: Int) =
        Chunk("cue ", ByteArray(4 + frames.size * 24).apply {
            p32(0, frames.size)
            frames.forEachIndexed { i, f ->
                p32(4 + i * 24, i)
                p32(4 + i * 24 + 20, f)
            }
        })

    private fun smpl(start: Int, end: Int) =
        Chunk("smpl", ByteArray(60).apply {
            p32(28, 1)
            p32(44, start)
            p32(48, end)
        })

    private fun assertNear(expected: Float, actual: Float) =
        assertTrue(abs(expected - actual) < 0.0001f, "Expected $expected but was $actual")

    @Test
    fun test_decodes_16_bit_mono() {
        val samples = decodeWav(wav(1, 1, 22050, 16, pcm16(0, 16384, -32768, 32767)))
        assertEquals(1, samples.channels)
        assertEquals(22050, samples.sampleRate)
        assertEquals(4, samples.frames)
        assertNear(0f, samples[0, 0])
        assertNear(0.5f, samples[1, 0])
        assertNear(-1f, samples[2, 0])
        assertNear(32767 / 32768f, samples[3, 0])
        assertFalse(samples.hasLoop)
        assertEquals(0, samples.markers.size)
    }

    @Test
    fun test_decodes_8_and_24_bit() {
        val eight = decodeWav(wav(1, 1, 8000, 8, byteArrayOf(128.toByte(), 192.toByte(), 0)))
        assertNear(0f, eight[0, 0])
        assertNear(0.5f, eight[1, 0])
        assertNear(-1f, eight[2, 0])

        val data = byteArrayOf(0, 0, 0x40, 0, 0, 0x80.toByte())
        val twentyFour = decodeWav(wav(1, 1, 8000, 24, data))
        assertNear(0.5f, twentyFour[0, 0])
        assertNear(-1f, twentyFour[1, 0])
    }

    @Test
    fun test_decodes_float_stereo() {
        val data = ByteArray(16)
        floatArrayOf(0.25f, -0.5f, 1f, -1f).forEachIndexed { i, f -> data.p32(i * 4, f.toBits()) }
        val samples = decodeWav(wav(3, 2, 48000, 32, data))
        assertEquals(2, samples.channels)
        assertEquals(2, samples.frames)
        assertContentEquals(floatArrayOf(0.25f, -0.5f, 1f, -1f), samples.samples)
        assertNear(-0.5f, samples[0, 1])
    }

    @Test
    fun test_reads_smpl_loop_and_cue_markers() {
        val bytes = wav(1, 1, 44100, 16, pcm16(*IntArray(100)), cue(80, 10, 40), smpl(20, 59))
        val samples = decodeWav(bytes)
        assertTrue(samples.hasLoop)
        assertEquals(20, samples.loopStart)
        assertEquals(60, samples.loopEnd)
        assertContentEquals(intArrayOf(10, 40, 80), samples.markers)
    }

    @Test
    fun test_ignores_a_loop_outside_the_samples() {
        val samples = decodeWav(wav(1, 1, 44100, 16, pcm16(*IntArray(10)), smpl(50, 70)))
        assertFalse(samples.hasLoop)
        assertEquals(-1, samples.loopStart)
        assertEquals(-1, samples.loopEnd)
    }

    @Test
    fun test_rejects_unsupported_wav() {
        assertFailsWith<UnsupportedFileTypeException> { decodeWav(ByteArray(4)) }
        assertFailsWith<UnsupportedFileTypeException> { decodeWav(wav(6, 1, 8000, 8, ByteArray(4))) }
    }

    @Test
    fun test_detects_formats_from_the_header() {
        assertEquals(AudioFileFormat.WAV, detectAudioFormat(wav(1, 1, 8000, 16, ByteArray(2))))
        assertEquals(AudioFileFormat.OGG, detectAudioFormat("OggS\u0000".encodeToByteArray()))
        assertEquals(AudioFileFormat.MP3, detectAudioFormat("ID3\u0004".encodeToByteArray()))
        assertEquals(AudioFileFormat.MP3, detectAudioFormat(byteArrayOf(0xFF.toByte(), 0xFB.toByte())))
        assertEquals(AudioFileFormat.UNKNOWN, detectAudioFormat(byteArrayOf(0xFF.toByte(), 0xF1.toByte())))
        assertEquals(AudioFileFormat.UNKNOWN, detectAudioFormat(ByteArray(0)))
    }

    @Test
    fun test_decode_audio_samples_reads_wav() = runTest {
        val samples = wav(1, 2, 44100, 16, pcm16(16384, -16384)).decodeAudioSamples()
        assertEquals(2, samples.channels)
        assertNear(0.5f, samples[0, 0])
        assertNear(-0.5f, samples[0, 1])
    }
}
