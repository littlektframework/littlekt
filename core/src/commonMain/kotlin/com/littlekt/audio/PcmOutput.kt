package com.littlekt.audio

import com.littlekt.Releasable
import kotlin.time.Duration

/**
 * Writes the samples of a [PcmOutput] each time it needs another buffer. On desktop it is called
 * from the audio thread, so guard any state shared with the game loop.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
fun interface PcmSource {
    /**
     * Fills [buffer] with [frames] frames of interleaved samples from `-1` to `1`. The buffer is
     * zeroed beforehand, so anything left unwritten is silence.
     */
    fun fill(buffer: FloatArray, frames: Int)
}

/**
 * A stream of raw samples written by a [PcmSource], such as a custom mixer or a synth. Create one
 * with [Audio.createPcmOutput]. It plays through that [Audio], so [Audio.masterVolume] applies and
 * it is released along with it.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
interface PcmOutput : Releasable {
    /** The number of frames played per second. */
    val sampleRate: Int

    /** The number of interleaved channels per frame, `1` or `2`. */
    val channels: Int

    /** The number of frames the [PcmSource] writes per buffer. */
    val bufferFrames: Int

    /** The number of buffers queued for playback. More buffers are safer but add latency. */
    val bufferCount: Int

    /** How far ahead of playback the samples are written. */
    val latency: Duration

    /** The volume of this output, from `0` to `1`. */
    var volume: Float

    /** `true` while playing. */
    val playing: Boolean

    /** The total number of frames written by the [PcmSource] so far. */
    val framesWritten: Long

    /** Starts or resumes playback. */
    fun play()

    /** Pauses playback, keeping the queued buffers. */
    fun pause()

    /** Stops playback and drops the queued buffers. [play] starts again with new buffers. */
    fun stop()

    companion object {
        /** The default [sampleRate]. */
        const val DEFAULT_SAMPLE_RATE = 44100

        /** The default [bufferFrames]. */
        const val DEFAULT_BUFFER_FRAMES = 1024

        /** The default [bufferCount]. */
        const val DEFAULT_BUFFER_COUNT = 4
    }
}
