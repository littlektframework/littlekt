package com.littlekt.audio

import com.littlekt.util.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.w3c.dom.Audio

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
class WebAudioClip(val assetPath: String) : AudioClip {

    override var volume = 1f
    override val duration: Duration
        get() = latestClip.duration.toDouble().seconds

    private val clipPool = mutableListOf(ClipWrapper())
    private var latestClip = clipPool.first()
    private var lastPlay = Double.NEGATIVE_INFINITY

    private fun nextClip(): ClipWrapper {
        for (i in clipPool.indices) {
            if (clipPool[i].clipState == ClipState.STOPPED) {
                return clipPool[i]
            }
        }
        if (clipPool.size < MAX_CLIP_POOL_SIZE) {
            val clip = ClipWrapper()
            clipPool += clip
            return clip
        }
        return clipPool.minByOrNull { it.startTime }!!
    }

    override fun play(volume: Float, loop: Boolean) {
        val t = now()
        lastPlay = t
        latestClip =
            nextClip().apply {
                this.volume = volume
                this.loop = loop
                play()
            }
    }

    override fun stop() {
        latestClip.stop()
    }

    override fun resume() {
        latestClip.resume()
    }

    override fun pause() {
        latestClip.pause()
    }

    override fun release() {
        latestClip.stop()
        clipPool.clear()
    }

    companion object {
        const val MAX_CLIP_POOL_SIZE = 5
    }

    private enum class ClipState {
        STOPPED,
        PLAYING
    }

    private inner class ClipWrapper {
        val audioElement = Audio(assetPath)

        var volume: Float
            get() = audioElement.volume.toFloat()
            set(value) {
                audioElement.volume = value.toDouble()
            }

        var currentTime: Float
            get() = audioElement.currentTime.toFloat()
            set(value) {
                audioElement.currentTime = value.toDouble()
            }

        val duration: Float
            get() = audioElement.duration.toFloat()

        var isPaused: Boolean = false
            private set

        var isStarted = false

        var loop: Boolean
            get() = audioElement.loop
            set(value) {
                audioElement.loop = value
            }

        var clipState = ClipState.STOPPED
            private set

        var startTime = 0.0

        init {
            audioElement.onended = {
                clipState = ClipState.STOPPED
                true.asDynamic()
            }
        }

        fun play() {
            audioElement.pause()
            currentTime = 0f
            clipState = ClipState.PLAYING
            isStarted = true
            audioElement.play()
        }

        fun stop() {
            isPaused = true
            audioElement.pause()
            clipState = ClipState.STOPPED
        }

        fun resume() {
            audioElement.pause()
            clipState = ClipState.PLAYING
            isStarted = true
            audioElement.play()
        }

        fun pause() {
            isPaused = true
            audioElement.pause()
            clipState = ClipState.STOPPED
        }
    }
}
