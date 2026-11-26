package com.littlekt.audio

import com.littlekt.log.Logger
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.time.Duration
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.EXTFloat32

/**
 * A [PcmOutput] that queues OpenAL buffers on a reserved source. The audio thread of the
 * [OpenALAudioContext] refills each buffer from the [PcmSource] once it has played.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
class OpenALPcmOutput
internal constructor(
    private val context: OpenALAudioContext,
    sampleRate: Int,
    channels: Int,
    bufferFrames: Int,
    bufferCount: Int,
    source: PcmSource,
) : PcmOutput {
    private val filler = PcmFiller(sampleRate, channels, bufferFrames, bufferCount, source)
    private val lock = Any()
    private val floats = context.supportsFloat32
    private val format =
        when {
            floats && channels == 2 -> EXTFloat32.AL_FORMAT_STEREO_FLOAT32
            floats -> EXTFloat32.AL_FORMAT_MONO_FLOAT32
            channels == 2 -> AL_FORMAT_STEREO16
            else -> AL_FORMAT_MONO16
        }
    private val floatData: FloatBuffer? =
        if (floats) BufferUtils.createFloatBuffer(filler.buffer.size) else null
    private val shortData: ShortBuffer? =
        if (floats) null else BufferUtils.createShortBuffer(filler.buffer.size)
    private val buffers = IntArray(bufferCount)
    private var sourceId = -1
    private var released = false

    @Volatile private var isPlaying = false

    /** Half the length of a buffer, in nanoseconds. How often the audio thread checks this. */
    internal val pollNanos: Long = bufferFrames * 500_000_000L / sampleRate

    override val sampleRate: Int = sampleRate
    override val channels: Int = channels
    override val bufferFrames: Int = bufferFrames
    override val bufferCount: Int = bufferCount
    override val latency: Duration = filler.latency

    override val framesWritten: Long
        get() = filler.framesWritten

    override val playing: Boolean
        get() = isPlaying

    override var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            val id = sourceId
            if (id != -1) alSourcef(id, AL_GAIN, field)
        }

    init {
        if (!context.NO_DEVICE) alGenBuffers(buffers)
    }

    override fun play() {
        synchronized(lock) {
            if (context.NO_DEVICE || released || isPlaying) return
            if (sourceId == -1) {
                sourceId = context.obtainSource(reserve = true)
                if (sourceId == -1) {
                    logger.warn { "Every source is reserved. Unable to play the PcmOutput." }
                    return
                }
                alSourcef(sourceId, AL_GAIN, volume)
            } else if (alGetSourcei(sourceId, AL_BUFFERS_QUEUED) > 0) {
                alSourcePlay(sourceId)
            }
            isPlaying = true
        }
        context.wakePcmThread()
    }

    override fun pause() {
        synchronized(lock) {
            isPlaying = false
            if (sourceId != -1) alSourcePause(sourceId)
        }
    }

    override fun stop() {
        synchronized(lock) {
            isPlaying = false
            if (sourceId == -1) return
            alSourceStop(sourceId)
            alSourcei(sourceId, AL_BUFFER, 0)
            context.releaseSource(sourceId)
            sourceId = -1
        }
    }

    override fun release() {
        if (released) return
        stop()
        context.removePcmOutput(this)
        synchronized(lock) {
            released = true
            if (!context.NO_DEVICE) alDeleteBuffers(buffers)
        }
    }

    /** Queues every buffer once playing starts, then refills each buffer that has played. */
    internal fun update() {
        synchronized(lock) {
            if (!isPlaying || sourceId == -1) return
            try {
                if (alGetSourcei(sourceId, AL_BUFFERS_QUEUED) == 0) {
                    for (i in buffers.indices) write(buffers[i])
                    alSourceQueueBuffers(sourceId, buffers)
                    alSourcePlay(sourceId)
                    return
                }
                var processed = alGetSourcei(sourceId, AL_BUFFERS_PROCESSED)
                while (processed-- > 0) {
                    val bufferId = alSourceUnqueueBuffers(sourceId)
                    write(bufferId)
                    alSourceQueueBuffers(sourceId, bufferId)
                }
                if (alGetSourcei(sourceId, AL_SOURCE_STATE) != AL_PLAYING) alSourcePlay(sourceId)
            } catch (ex: Throwable) {
                isPlaying = false
                alSourcePause(sourceId)
                logger.error { "PcmSource failed, pausing its PcmOutput: $ex" }
            }
        }
    }

    private fun write(bufferId: Int) {
        val samples = filler.fill()
        if (floatData != null) {
            floatData.clear()
            floatData.put(samples).flip()
            alBufferData(bufferId, format, floatData, sampleRate)
        } else if (shortData != null) {
            shortData.clear()
            for (i in samples.indices) {
                shortData.put((samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
            }
            shortData.flip()
            alBufferData(bufferId, format, shortData, sampleRate)
        }
    }

    companion object {
        private val logger = Logger<OpenALPcmOutput>()
    }
}
