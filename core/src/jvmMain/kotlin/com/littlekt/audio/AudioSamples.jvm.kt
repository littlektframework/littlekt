package com.littlekt.audio

import com.littlekt.file.UnsupportedFileTypeException
import fr.delthas.javamp3.Sound
import java.io.ByteArrayInputStream
import org.lwjgl.stb.STBVorbis
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

internal actual suspend fun decodeCompressedAudio(
    bytes: ByteArray,
    format: AudioFileFormat,
): AudioSamples =
    when (format) {
        AudioFileFormat.OGG -> decodeOgg(bytes)
        AudioFileFormat.MP3 -> decodeMp3(bytes)
        else -> throw UnsupportedFileTypeException("Only WAV, OGG & MP3 audio can be decoded!")
    }

private fun decodeOgg(bytes: ByteArray): AudioSamples {
    val data = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
    try {
        return MemoryStack.stackPush().use { stack ->
            val error = stack.mallocInt(1)
            val handle = STBVorbis.stb_vorbis_open_memory(data, error, null)
            if (handle == 0L) {
                throw UnsupportedFileTypeException("Unable to decode OGG Vorbis, error ${error[0]}!")
            }
            try {
                val info = STBVorbis.stb_vorbis_get_info(handle, STBVorbisInfo.malloc(stack))
                val channels = info.channels()
                val sampleRate = info.sample_rate()
                var capacity = STBVorbis.stb_vorbis_stream_length_in_samples(handle) * channels
                if (capacity <= 0) capacity = sampleRate * channels
                var samples = FloatArray(capacity)
                var written = 0
                val chunk = FloatArray(OGG_CHUNK_FRAMES * channels)
                while (true) {
                    val frames = STBVorbis.stb_vorbis_get_samples_float_interleaved(handle, channels, chunk)
                    if (frames <= 0) break
                    val count = frames * channels
                    if (written + count > samples.size) {
                        samples = samples.copyOf(maxOf(samples.size * 2, written + count))
                    }
                    chunk.copyInto(samples, written, 0, count)
                    written += count
                }
                AudioSamples(
                    if (written == samples.size) samples else samples.copyOf(written),
                    channels,
                    sampleRate,
                )
            } finally {
                STBVorbis.stb_vorbis_close(handle)
            }
        }
    } finally {
        MemoryUtil.memFree(data)
    }
}

private fun decodeMp3(bytes: ByteArray): AudioSamples {
    val sound = Sound(ByteArrayInputStream(bytes))
    val raw = sound.use { it.readBytes() }
    val channels = if (sound.isStereo) 2 else 1
    val sampleRate = sound.samplingFrequency
    val samples = FloatArray(raw.size / 2 - (raw.size / 2) % channels)
    for (i in samples.indices) {
        val lo = raw[i * 2].toInt() and 0xFF
        val hi = raw[i * 2 + 1].toInt()
        samples[i] = ((hi shl 8) or lo).toShort() / 32768f
    }
    return AudioSamples(samples, channels, sampleRate)
}

private const val OGG_CHUNK_FRAMES = 4096
