package com.lehaine.littlekt.audio

import com.lehaine.littlekt.audio.OpenALContext.NO_DEVICE
import com.lehaine.littlekt.log.Logger
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.SOFTDirectChannels
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioClip(pcm: ByteArray, val channels: Int, val sampleRate: Int) : AudioClip {

    private var bufferID = -1

    override var volume: Float = 1f
    override val duration: Duration

    init {
        if(NO_DEVICE) {
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
        val sourceId = obtainSource()

        if (sourceId == -1) return

        alSourcei(sourceId, AL_BUFFER, bufferID)
        alSourcei(sourceId, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcef(sourceId, AL_GAIN, volume)
        alSourcePlay(sourceId)
    }

    override fun stop() = withDevice {
        sources.forEach {
            if (alGetSourcei(it, AL_BUFFER) == bufferID) {
                alSourceStop(it)
            }
        }
    }

    override fun resume() = withDevice {
        sources.forEach {
            if (alGetSourcei(it, AL_BUFFER) == bufferID) {
                alSourcePlay(it)
            }
        }
    }

    override fun pause() = withDevice {
        sources.forEach {
            if (alGetSourcei(it, AL_BUFFER) == bufferID) {
                alSourcePause(it)
            }
        }
    }

    override fun dispose() = withDevice {
        if (bufferID == -1) return

        sources.forEach {
            if (alGetSourcei(it, AL_BUFFER) == bufferID) {
                alSourceStop(it)
                alSourcei(it, AL_BUFFER, 0)
            }
        }
        alDeleteBuffers(bufferID)
        bufferID = -1
    }


    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    private fun obtainSource(): Int {
        if (NO_DEVICE) return 0

        return sources.asSequence()
            .map { it to alGetSourcei(it, AL_SOURCE_STATE) }
            .firstOrNull { (_, state) -> state != AL_PLAYING && state != AL_PAUSED }
            ?.let { (sourceId, _) ->
                alSourceStop(sourceId)
                alSourcei(sourceId, AL_BUFFER, 0)
                alSourcef(sourceId, AL_GAIN, 1f)
                alSourcef(sourceId, AL_PITCH, 1f)
                alSource3f(sourceId, AL_POSITION, 0f, 0f, 1f)
                alSourcei(sourceId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL_TRUE)
                sourceId
            } ?: -1
    }

    companion object {
        private val logger = Logger<OpenALAudioClip>()
        private val sources by lazy {
            val result = mutableListOf<Int>()
            for (i in 0 until 4) { // total simultaneous sources
                val sourceID: Int = alGenSources()
                if (alGetError() != AL_NO_ERROR) break
                result.add(sourceID)
            }
            result
        }
    }
}