package com.littlekt.audio

import com.littlekt.log.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.lwjgl.openal.AL10.*

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioClip(
    private val context: OpenALAudioContext,
    pcm: ByteArray,
    val channels: Int,
    val sampleRate: Int
) : AudioClip {

    private var bufferID = -1

    override var volume: Float = 1f
    override val duration: Duration

    private val NO_DEVICE
        get() = context.NO_DEVICE

    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }
        val bytes = pcm.size - (pcm.size % (if (channels > 1) 4 else 2))
        val samples = bytes / (2 * channels)
        duration = (samples / sampleRate.toDouble()).seconds

        val buffer =
            ByteBuffer.allocateDirect(bytes).apply {
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

    private var warnedStereo = false

    override fun play(volume: Float, loop: Boolean): Long {
        val sourceId = obtain(volume, loop)
        if (sourceId == -1) return -1L
        alSourcePlay(sourceId)
        return context.soundId(sourceId)
    }

    override fun playAt(
        x: Float,
        y: Float,
        z: Float,
        volume: Float,
        loop: Boolean,
        attenuation: AudioAttenuation,
    ): Long {
        val sourceId = obtain(volume, loop)
        if (sourceId == -1) return -1L
        if (channels > 1 && !warnedStereo) {
            warnedStereo = true
            logger.warn { "Stereo clips can't be positioned. Use a mono clip with playAt." }
        }
        context.spatialize(sourceId, x, y, z, attenuation)
        alSourcePlay(sourceId)
        return context.soundId(sourceId)
    }

    override fun setPosition(id: Long, x: Float, y: Float, z: Float) {
        val sourceId = context.findSource(id, bufferID)
        if (sourceId != -1) alSource3f(sourceId, AL_POSITION, x, y, z)
    }

    override fun setVolume(id: Long, volume: Float) {
        val sourceId = context.findSource(id, bufferID)
        if (sourceId != -1) alSourcef(sourceId, AL_GAIN, volume)
    }

    override fun stop(id: Long) {
        val sourceId = context.findSource(id, bufferID)
        if (sourceId != -1) alSourceStop(sourceId)
    }

    override fun isPlaying(id: Long): Boolean {
        val sourceId = context.findSource(id, bufferID)
        return sourceId != -1 && alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING
    }

    private fun obtain(volume: Float, loop: Boolean): Int {
        if (NO_DEVICE || bufferID == -1) return -1
        val sourceId = context.obtainSource()
        if (sourceId == -1) return -1
        alSourcei(sourceId, AL_BUFFER, bufferID)
        alSourcei(sourceId, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        alSourcef(sourceId, AL_GAIN, volume)
        return sourceId
    }

    override fun stop() = withDevice { context.stopSourceViaBufferID(bufferID) }

    override fun resume() = withDevice { context.resumeSourceViaBufferID(bufferID) }

    override fun pause() = withDevice { context.pauseSourceViaBufferID(bufferID) }

    override fun release() = withDevice {
        if (bufferID == -1) return@withDevice

        context.disposeSourceViaBufferID(bufferID)
        bufferID = -1
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    companion object {
        private val logger = Logger<OpenALAudioClip>()
    }
}
