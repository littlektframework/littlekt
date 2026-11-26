package com.littlekt.audio

import com.littlekt.file.UnsupportedFileTypeException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Decoded audio as interleaved samples from `-1` to `1`, such as from [decodeAudioSamples].
 *
 * @property samples the interleaved samples, [channels] per frame
 * @property channels the number of channels per frame
 * @property sampleRate the number of frames per second
 * @property loopStart the first frame of the loop from a WAV `smpl` chunk, or `-1` if there is none
 * @property loopEnd the frame after the last frame of the loop, or `-1` if there is none
 * @property markers the sorted frames of the WAV `cue ` points. Empty if there are none.
 * @author Colton Daily
 * @date 11/26/2026
 */
class AudioSamples(
    val samples: FloatArray,
    val channels: Int,
    val sampleRate: Int,
    val loopStart: Int = -1,
    val loopEnd: Int = -1,
    val markers: IntArray = IntArray(0),
) {
    init {
        require(channels > 0) { "channels must be greater than 0!" }
        require(sampleRate > 0) { "sampleRate must be greater than 0!" }
        require((loopStart < 0) == (loopEnd < 0)) { "loopStart & loopEnd must both be set or -1!" }
        require(loopStart < 0 || loopEnd > loopStart) { "loopEnd must be greater than loopStart!" }
    }

    /** The number of frames. */
    val frames: Int
        get() = samples.size / channels

    /** The length of the audio. */
    val duration: Duration
        get() = (frames.toDouble() / sampleRate).seconds

    /** `true` if [loopStart] & [loopEnd] are set. */
    val hasLoop: Boolean
        get() = loopStart >= 0

    /** @return the sample of [channel] at [frame] */
    operator fun get(frame: Int, channel: Int): Float = samples[frame * channels + channel]
}

/**
 * Decodes WAV, OGG Vorbis or MP3 bytes into [AudioSamples]. The format is read from the header.
 * WAV `smpl` loop points and `cue ` markers are kept. Uses `decodeAudioData` on the web, which
 * also decodes any other format the browser supports.
 */
suspend fun ByteArray.decodeAudioSamples(): AudioSamples {
    val format = detectAudioFormat(this)
    return if (format == AudioFileFormat.WAV) decodeWav(this) else decodeCompressedAudio(this, format)
}

/**
 * Decodes the bytes of a RIFF WAVE file into [AudioSamples]. Supports 8, 16, 24 & 32 bit PCM and 32
 * & 64 bit float. The first `smpl` loop sets the loop points and `cue ` points become markers.
 */
fun decodeWav(bytes: ByteArray): AudioSamples {
    if (bytes.size < 12 || !bytes.isTag(0, "RIFF") || !bytes.isTag(8, "WAVE")) {
        throw UnsupportedFileTypeException("Not a WAVE file!")
    }
    var format = 0
    var channels = 0
    var sampleRate = 0
    var bits = 0
    var dataOffset = -1
    var dataLength = 0
    var cueOffset = -1
    var loopStart = -1L
    var loopEnd = -1L
    var offset = 12
    while (offset + 8 <= bytes.size) {
        val size = bytes.u32(offset + 4)
        val body = offset + 8
        val length = minOf(size, (bytes.size - body).toLong()).toInt()
        when {
            bytes.isTag(offset, "fmt ") && length >= 16 -> {
                format = bytes.u16(body)
                channels = bytes.u16(body + 2)
                sampleRate = bytes.u32(body + 4).toInt()
                bits = bytes.u16(body + 14)
                if (format == WAVE_FORMAT_EXTENSIBLE && length >= 26) format = bytes.u16(body + 24)
            }
            bytes.isTag(offset, "data") -> {
                dataOffset = body
                dataLength = if (size == 0L || size == 0xFFFFFFFFL) bytes.size - body else length
            }
            bytes.isTag(offset, "cue ") && length >= 4 -> cueOffset = body
            bytes.isTag(offset, "smpl") && length >= 60 && bytes.u32(body + 28) > 0 -> {
                loopStart = bytes.u32(body + 44)
                loopEnd = bytes.u32(body + 48) + 1
            }
        }
        val next = body + size + (size and 1)
        if (next > bytes.size) break
        offset = next.toInt()
    }
    if (dataOffset < 0 || channels <= 0 || sampleRate <= 0) {
        throw UnsupportedFileTypeException("WAVE file is missing its fmt or data chunk!")
    }
    val float = format == WAVE_FORMAT_FLOAT
    if ((format != WAVE_FORMAT_PCM && !float) || (float && bits != 32 && bits != 64)) {
        throw UnsupportedFileTypeException("WAVE format $format with $bits bits isn't supported!")
    }
    if (!float && bits != 8 && bits != 16 && bits != 24 && bits != 32) {
        throw UnsupportedFileTypeException("WAVE files with $bits bits aren't supported!")
    }
    val bytesPerSample = bits / 8
    val frames = dataLength / (bytesPerSample * channels)
    val samples = FloatArray(frames * channels)
    var p = dataOffset
    for (i in samples.indices) {
        samples[i] =
            when {
                float && bits == 32 -> Float.fromBits(bytes.u32(p).toInt())
                float -> Double.fromBits(bytes.u32(p) or (bytes.u32(p + 4) shl 32)).toFloat()
                bits == 8 -> ((bytes[p].toInt() and 0xFF) - 128) / 128f
                bits == 16 -> bytes.u16(p).toShort() / 32768f
                bits == 24 ->
                    ((bytes[p].toInt() and 0xFF) or
                        ((bytes[p + 1].toInt() and 0xFF) shl 8) or
                        (bytes[p + 2].toInt() shl 16)) / 8388608f
                else -> bytes.u32(p).toInt() / 2147483648f
            }
        p += bytesPerSample
    }
    val hasLoop = loopStart in 0 until frames && loopEnd > loopStart
    val markers =
        if (cueOffset < 0) IntArray(0)
        else {
            val count = minOf(bytes.u32(cueOffset), ((bytes.size - cueOffset - 4) / 24).toLong())
            IntArray(count.toInt()) {
                bytes.u32(cueOffset + 4 + it * 24 + 20).coerceAtMost(frames.toLong()).toInt()
            }
                .apply { sort() }
        }
    return AudioSamples(
        samples,
        channels,
        sampleRate,
        if (hasLoop) loopStart.toInt() else -1,
        if (hasLoop) loopEnd.coerceAtMost(frames.toLong()).toInt() else -1,
        markers,
    )
}

/** The audio file formats [decodeAudioSamples] knows from their header. */
internal enum class AudioFileFormat {
    WAV,
    OGG,
    MP3,
    UNKNOWN,
}

internal fun detectAudioFormat(bytes: ByteArray): AudioFileFormat =
    when {
        bytes.size >= 12 && bytes.isTag(0, "RIFF") && bytes.isTag(8, "WAVE") -> AudioFileFormat.WAV
        bytes.size >= 4 && bytes.isTag(0, "OggS") -> AudioFileFormat.OGG
        bytes.size >= 3 && bytes.isTag(0, "ID3") -> AudioFileFormat.MP3
        bytes.size >= 2 &&
            (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xE0) == 0xE0 &&
            (bytes[1].toInt() and 0x06) != 0 -> AudioFileFormat.MP3
        else -> AudioFileFormat.UNKNOWN
    }

/** Decodes the compressed [bytes] with the platform decoder. */
internal expect suspend fun decodeCompressedAudio(
    bytes: ByteArray,
    format: AudioFileFormat,
): AudioSamples

private const val WAVE_FORMAT_PCM = 1
private const val WAVE_FORMAT_FLOAT = 3
private const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE

private fun ByteArray.isTag(offset: Int, tag: String): Boolean {
    for (i in tag.indices) {
        if (this[offset + i].toInt() != tag[i].code) return false
    }
    return true
}

private fun ByteArray.u16(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.u32(offset: Int): Long =
    (u16(offset).toLong() or (u16(offset + 2).toLong() shl 16))
