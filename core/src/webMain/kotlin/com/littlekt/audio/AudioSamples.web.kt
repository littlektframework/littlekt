package com.littlekt.audio

import com.littlekt.async.await
import com.littlekt.file.compression.toUint8Array
import org.khronos.webgl.get

internal actual suspend fun decodeCompressedAudio(
    bytes: ByteArray,
    format: AudioFileFormat,
): AudioSamples {
    val sampleRate = nativeSampleRate(bytes, format)
    val context = OfflineAudioContext(1, 1, sampleRate)
    val buffer = context.decodeAudioData(toUint8Array(bytes).buffer).await<AudioBuffer>()
    val channels = buffer.numberOfChannels
    val frames = buffer.length
    val samples = FloatArray(frames * channels)
    for (c in 0 until channels) {
        val data = buffer.getChannelData(c)
        for (f in 0 until frames) samples[f * channels + c] = data[f]
    }
    return AudioSamples(samples, channels, buffer.sampleRate.toInt())
}

/**
 * Reads the sample rate from the header, since `decodeAudioData` resamples to the rate of its
 * context.
 */
private fun nativeSampleRate(bytes: ByteArray, format: AudioFileFormat): Int =
    when (format) {
        AudioFileFormat.OGG -> oggSampleRate(bytes)
        AudioFileFormat.MP3 -> mp3SampleRate(bytes)
        else -> 0
    }.takeIf { it in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE } ?: PcmOutput.DEFAULT_SAMPLE_RATE

private fun oggSampleRate(bytes: ByteArray): Int {
    if (bytes.size < 28) return 0
    val packet = 27 + (bytes[26].toInt() and 0xFF)
    if (bytes.size < packet + 16) return 0
    if (bytes.hasText(packet, "OpusHead")) return OPUS_SAMPLE_RATE
    if (bytes[packet].toInt() != 1 || !bytes.hasText(packet + 1, "vorbis")) return 0
    return bytes.u32(packet + 12)
}

private fun mp3SampleRate(bytes: ByteArray): Int {
    var offset = 0
    if (bytes.size >= 10 && bytes.hasText(0, "ID3")) {
        offset =
            10 +
                ((bytes[6].toInt() and 0x7F) shl 21) +
                ((bytes[7].toInt() and 0x7F) shl 14) +
                ((bytes[8].toInt() and 0x7F) shl 7) +
                (bytes[9].toInt() and 0x7F)
        if ((bytes[5].toInt() and 0x10) != 0) offset += 10
    }
    while (offset + 4 <= bytes.size) {
        val b1 = bytes[offset + 1].toInt() and 0xFF
        val b2 = bytes[offset + 2].toInt() and 0xFF
        if ((bytes[offset].toInt() and 0xFF) == 0xFF && (b1 and 0xE0) == 0xE0 && (b1 and 0x06) != 0) {
            val version = (b1 shr 3) and 3
            val index = (b2 shr 2) and 3
            if (version != 1 && index != 3) {
                val rate = MP3_SAMPLE_RATES[index]
                return when (version) {
                    3 -> rate
                    2 -> rate / 2
                    else -> rate / 4
                }
            }
        }
        offset++
    }
    return 0
}

private fun ByteArray.hasText(offset: Int, text: String): Boolean {
    if (offset + text.length > size) return false
    for (i in text.indices) {
        if (this[offset + i].toInt() != text[i].code) return false
    }
    return true
}

private fun ByteArray.u32(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private val MP3_SAMPLE_RATES = intArrayOf(44100, 48000, 32000)
private const val OPUS_SAMPLE_RATE = 48000
private const val MIN_SAMPLE_RATE = 8000
private const val MAX_SAMPLE_RATE = 192000
