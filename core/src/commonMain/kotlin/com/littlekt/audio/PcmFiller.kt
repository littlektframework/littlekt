package com.littlekt.audio

import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Validates the settings of a [PcmOutput] and fills its reused buffer from a [PcmSource].
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class PcmFiller(
    val sampleRate: Int,
    val channels: Int,
    val bufferFrames: Int,
    val bufferCount: Int,
    private val source: PcmSource,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be greater than 0!" }
        require(channels == 1 || channels == 2) { "channels must be 1 or 2!" }
        require(bufferFrames > 0) { "bufferFrames must be greater than 0!" }
        require(bufferCount >= 2) { "bufferCount must be at least 2!" }
    }

    /** The interleaved samples of the last [fill]. */
    val buffer = FloatArray(bufferFrames * channels)

    /** The latency of [bufferCount] buffers of [bufferFrames]. */
    val latency: Duration = (bufferFrames.toDouble() * bufferCount / sampleRate).seconds

    @Volatile
    var framesWritten = 0L
        private set

    /** Zeroes [buffer] and has the [source] write the next [bufferFrames] frames into it. */
    fun fill(): FloatArray {
        buffer.fill(0f)
        source.fill(buffer, bufferFrames)
        framesWritten += bufferFrames
        return buffer
    }
}
