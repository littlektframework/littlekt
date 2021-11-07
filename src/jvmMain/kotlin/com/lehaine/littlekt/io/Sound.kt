package com.lehaine.littlekt.io

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
actual class Sound(private val buffer: ShortBuffer, val channels: Int, val sampleRate: Int) {

    var bufferID: Int = -1
    var volume = 1.0f

    init {
        if (!NO_DEVICE) {
            bufferID = alGenBuffers()
            alBufferData(
                bufferID,
                if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16,
                buffer,
                sampleRate
            )
        }
    }

    actual fun play(loop: Int) = withDevice {
        val sourceID: Int = obtainSource()

        // In case it still didn't work
        if (sourceID == -1) return

        alSourcei(sourceID, AL_BUFFER, bufferID)
        alSourcei(sourceID, AL_LOOPING, AL_FALSE)
        alSourcef(sourceID, AL_GAIN, volume)
        alSourcePlay(sourceID)
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
                sourceId
            } ?: -1
    }

    companion object {
        val NO_DEVICE = try {
            val byteBuffer: ByteBuffer? = null
            val device = ALC10.alcOpenDevice(byteBuffer)
            val deviceCaps = ALC.createCapabilities(device)
            val context = ALC10.alcCreateContext(device, null as IntBuffer?)
            ALC10.alcMakeContextCurrent(context)
            AL.createCapabilities(deviceCaps)
            false
        } catch (ex: Exception) {
            ex.printStackTrace()
            true
        }

        private val sources by lazy {
            val result = mutableListOf<Int>()
            for (i in 0 until 4) {
                val sourceID: Int = alGenSources()
                if (alGetError() != AL_NO_ERROR) break
                result.add(sourceID)
            }
            result
        }
    }
}