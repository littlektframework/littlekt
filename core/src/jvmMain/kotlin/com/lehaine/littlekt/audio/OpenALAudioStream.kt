package com.lehaine.littlekt.audio

import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.seconds
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.*
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioStream(
    private val context: OpenALAudioContext,
    private val read: (ByteArray) -> Int,
    val channels: Int,
    val sampleRate: Int
) : AudioStream {
    private val NO_DEVICE get() = context.NO_DEVICE

    private val tempBytes = ByteArray(BUFFER_SIZE)
    private val tempBuffer = BufferUtils.createByteBuffer(BUFFER_SIZE)

    private var sourceID = -1

    private val maxSecondsToBuffer = (BUFFER_SIZE.toFloat() / (BYTES_PER_SAMPLE * channels * sampleRate)).seconds
    private var buffers: IntBuffer? = null

    override var volume: Float = 1f


    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }
    }


    override fun play(volume: Float, loop: Boolean) = withDevice {
        if (sourceID == -1) {
            sourceID = context.obtainSource()
            if (sourceID == -1) return@withDevice

            if (buffers == null) {
                buffers = BufferUtils.createIntBuffer(BUFFER_COUNT).also {
                    alGetError()
                    alGenBuffers(it)
                    val errorCode = alGetError()
                    if (errorCode != AL_NO_ERROR) {
                        error("Unable to allocate audio buffers. AL Error: $errorCode")
                    }
                }
            }
            alSourcei(sourceID, AL_LOOPING, AL_FALSE)
            alSourcef(sourceID, AL_GAIN, volume)

            alGetError()

            var filled = false // check if there is anything to play
            buffers?.let { buffers ->
                for (i in 0 until BUFFER_COUNT) {
                    val bufferID = buffers.get(i)
                    if (!fill(bufferID)) break
                    filled = true
                    alSourceQueueBuffers(sourceID, bufferID)
                }
            }
            // TODO
        }

    }

    override fun stop() = withDevice {

    }

    override fun resume() = withDevice {

    }

    override fun pause() = withDevice {

    }

    override fun dispose() = withDevice {
        stop()
        buffers?.let {
            alDeleteBuffers(it)
        } ?: return@withDevice
        buffers = null
    }

    private fun fill(bufferID: Int): Boolean {
        tempBuffer.clear()
        val length = read(tempBytes)
        if (length <= 0) {
            // TODO
        }

        // TODO
        return true
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    companion object {
        private val logger = Logger<OpenALAudioStream>()
        private const val BUFFER_SIZE = 4096 * 10
        private const val BUFFER_COUNT = 3
        private const val BYTES_PER_SAMPLE = 2
    }
}