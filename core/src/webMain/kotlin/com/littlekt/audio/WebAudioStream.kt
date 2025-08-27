package com.littlekt.audio

import org.w3c.dom.Audio

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
class WebAudioStream(val assetPath: String) : AudioStream {
    private val audioElement = Audio(assetPath)

    private var currentTime: Float
        get() = audioElement.currentTime.toFloat()
        set(value) {
            audioElement.currentTime = value.toDouble()
        }

    private var isPaused: Boolean = false
    private var isStarted = false

    private var clipState = StreamState.STOPPED

    override var looping: Boolean
        get() = audioElement.loop
        set(value) {
            audioElement.loop = value
        }

    override val playing: Boolean
        get() = clipState == StreamState.PLAYING

    override var volume: Float
        get() = audioElement.volume.toFloat()
        set(value) {
            audioElement.volume = value.toDouble()
        }

    init {
        audioElement.onended = {
            clipState = StreamState.STOPPED
        }
    }

    override suspend fun play(volume: Float, loop: Boolean) {
        audioElement.pause()
        this.volume = volume
        this.looping = loop
        currentTime = 0f
        clipState = StreamState.PLAYING
        isStarted = true
        audioElement.play()
    }

    override fun stop() {
        isPaused = true
        audioElement.pause()
        clipState = StreamState.STOPPED
    }

    override fun resume() {
        audioElement.pause()
        clipState = StreamState.PLAYING
        isStarted = true
        audioElement.play()
    }

    override fun pause() {
        stop()
    }

    override fun release() {
        stop()
    }

    private enum class StreamState {
        STOPPED,
        PLAYING
    }
}
