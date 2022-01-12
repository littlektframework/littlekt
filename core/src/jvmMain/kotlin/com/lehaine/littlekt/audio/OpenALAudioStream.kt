package com.lehaine.littlekt.audio

import com.lehaine.littlekt.log.Logger
import org.lwjgl.openal.AL10.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioStream(
    private val context: OpenALAudioContext,
    pcm: ByteArray,
    val channels: Int,
    val sampleRate: Int
) : AudioStream {

    private var bufferID = -1

    override var volume: Float = 1f
    override val duration: Duration

    private val NO_DEVICE get() = context.NO_DEVICE

    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }

        val bytes = pcm.size - (pcm.size % (if (channels > 1) 4 else 2))
        val samples = bytes / (2 * channels)
        duration = (samples / sampleRate.toDouble()).seconds

        val buffer = ByteBuffer.allocateDirect(bytes).apply {
            order(ByteOrder.nativeOrder())
            put(pcm, 0, bytes)
            flip()
        }
        if (bufferID == -1) {
            bufferID = alGenBuffers()
            alBufferData(
                bufferID,
                (if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16),
                buffer.asShortBuffer(),
                sampleRate
            )
        }
    }


    override fun play(volume: Float, loop: Boolean) = withDevice {
        val sourceId = context.obtainSource()

        if (sourceId == -1) return

        alSourcei(sourceId, AL_BUFFER, bufferID)
        alSourcei(sourceId, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcef(sourceId, AL_GAIN, volume)
        alSourcePlay(sourceId)
    }

    override fun stop() = withDevice {
        context.stopSource(bufferID)
    }

    override fun resume() = withDevice {
        context.resumeSource(bufferID)
    }

    override fun pause() = withDevice {
        context.pauseSource(bufferID)
    }

    override fun dispose() = withDevice {
        if (bufferID == -1) return

        context.disposeSource(bufferID)
        bufferID = -1
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    companion object {
        private val logger = Logger<OpenALAudioStream>()
    }
}