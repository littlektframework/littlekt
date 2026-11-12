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

    override var volume: Float = 1f
        set(value) {
            field = value
            applyVolume()
        }

    private val volumeListener: () -> Unit = { applyVolume() }

    private fun applyVolume() {
        audioElement.volume = (volume * WebAudio.masterVolume).toDouble()
    }

    init {
        WebAudio.addVolumeListener(volumeListener)
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
        WebAudio.removeVolumeListener(volumeListener)
    }

    private enum class StreamState {
        STOPPED,
        PLAYING
    }
}
