package com.lehaine.littlekt.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
object OpenALContext {
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

    fun destroy() {
        if (context != -1L) {
            ALC10.alcDestroyContext(context)
        }
        if (device != -1L) {
            ALC10.alcCloseDevice(device)
        }
    }
}