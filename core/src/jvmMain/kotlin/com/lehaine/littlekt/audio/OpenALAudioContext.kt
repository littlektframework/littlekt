package com.lehaine.littlekt.audio

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.util.fastForEach
import org.lwjgl.openal.*
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class OpenALAudioContext : Disposable {
    val device = try {
        val byteBuffer: ByteBuffer? = null
        ALC10.alcOpenDevice(byteBuffer)
    } catch (ex: Exception) {
        ex.printStackTrace()
        -1
    }
    val context: Long = try {
        val deviceCaps = ALC.createCapabilities(device)
        val context = ALC10.alcCreateContext(device, null as IntBuffer?)
        ALC10.alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
        context
    } catch (ex: Exception) {
        ex.printStackTrace()
        -1
    }

    val NO_DEVICE get() = context == -1L || device == -1L

    internal val audioStreams = mutableListOf<OpenALAudioStream>()

    private val sources by lazy {
        val result = mutableListOf<Int>()
        for (i in 0 until 4) { // total simultaneous sources
            val sourceID: Int = AL10.alGenSources()
            if (AL10.alGetError() != AL10.AL_NO_ERROR) break
            result.add(sourceID)
        }
        result
    }

    fun update() {
        if (NO_DEVICE) return
        audioStreams.fastForEach { it.update() }
    }

    fun obtainSource(): Int {
        if (NO_DEVICE) return 0

        return sources.asSequence()
            .map { it to AL10.alGetSourcei(it, AL10.AL_SOURCE_STATE) }
            .firstOrNull { (_, state) -> state != AL10.AL_PLAYING && state != AL10.AL_PAUSED }
            ?.let { (sourceId, _) ->
                AL10.alSourceStop(sourceId)
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0)
                AL10.alSourcef(sourceId, AL10.AL_GAIN, 1f)
                AL10.alSourcef(sourceId, AL10.AL_PITCH, 1f)
                AL10.alSource3f(sourceId, AL10.AL_POSITION, 0f, 0f, 1f)
                AL10.alSourcei(sourceId, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
                sourceId
            } ?: -1
    }

    fun resumeSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePlay(it)
            }
        }
    }

    fun stopSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
            }
        }
    }

    fun pauseSourceViaBufferID(bufferID: Int) {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourcePause(it)
            }
        }
    }

    fun disposeSourceViaBufferID(bufferID: Int) {
        if (bufferID == -1) return

        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == bufferID) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        AL10.alDeleteBuffers(bufferID)
    }

    override fun dispose() {
        sources.forEach {
            if (AL10.alGetSourcei(it, AL10.AL_BUFFER) == it) {
                AL10.alSourceStop(it)
                AL10.alSourcei(it, AL10.AL_BUFFER, 0)
            }
        }
        if (context != -1L) {
            ALC10.alcDestroyContext(context)
        }
        if (device != -1L) {
            ALC10.alcCloseDevice(device)
        }
    }
}