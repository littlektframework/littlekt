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
    private val volumeListener: () -> Unit = { clipPool.forEach { it.applyVolume() } }

    init {
        WebAudio.addVolumeListener(volumeListener)
    }
    private var latestClip = clipPool.first()
    private var lastPlay = Double.NEGATIVE_INFINITY
    private var nextId = 0L

    private fun nextClip(): ClipWrapper {
        for (i in clipPool.indices) {
            if (clipPool[i].clipState == ClipState.STOPPED) {
                return clipPool[i]
            }
        }
        if (clipPool.size < WebAudio.maxInstancesPerClip) {
            val clip = ClipWrapper()
            clipPool += clip
            return clip
        }
        return clipPool.minByOrNull { it.startTime }!!
    }

    override fun play(volume: Float, loop: Boolean): Long {
        lastPlay = now()
        latestClip =
            nextClip().apply {
                id = nextId++
                unspatialize()
                this.volume = volume
                this.loop = loop
                play()
            }
        return latestClip.id
    }

    override fun playAt(
        x: Float,
        y: Float,
        z: Float,
        volume: Float,
        loop: Boolean,
        attenuation: AudioAttenuation,
    ): Long {
        lastPlay = now()
        latestClip =
            nextClip().apply {
                id = nextId++
                spatialize(x, y, z, attenuation)
                this.volume = volume
                this.loop = loop
                play()
            }
        return latestClip.id
    }

    override fun setPosition(id: Long, x: Float, y: Float, z: Float) {
        find(id)?.setPosition(x, y, z)
    }

    override fun setVolume(id: Long, volume: Float) {
        find(id)?.volume = volume
    }

    override fun stop(id: Long) {
        find(id)?.stop()
    }

    override fun isPlaying(id: Long): Boolean = find(id) != null

    private fun find(id: Long): ClipWrapper? {
        if (id < 0L) return null
        for (i in clipPool.indices) {
            val clip = clipPool[i]
            if (clip.id == id && clip.clipState == ClipState.PLAYING) return clip
        }
        return null
    }

    override fun stop() {
        for (i in clipPool.indices) clipPool[i].stop()
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
        WebAudio.removeVolumeListener(volumeListener)
    }

    private enum class ClipState {
        STOPPED,
        PLAYING
    }

    private inner class ClipWrapper {
        val audioElement = Audio(assetPath)

        var id = -1L

        private var source: AudioNode? = null
        private var gain: GainNode? = null
        private var panner: PannerNode? = null
        private var spatial = false

        var volume: Float = 1f
            set(value) {
                field = value
                applyVolume()
            }

        fun applyVolume() {
            val volume = (volume * WebAudio.masterVolume).toDouble()
            val gain = gain
            if (gain != null) {
                audioElement.volume = 1.0
                gain.gain.value = volume
            } else {
                audioElement.volume = volume
            }
        }

        fun spatialize(x: Float, y: Float, z: Float, attenuation: AudioAttenuation) {
            val context = WebAudio.obtainAudioContext()
            val source = source ?: context.createMediaElementSource(audioElement).also { source = it }
            val gain = gain ?: context.createGain().also { gain = it }
            val panner =
                panner ?: context.createPanner().also {
                    it.panningModel = "equalpower"
                    panner = it
                }
            panner.distanceModel =
                when (attenuation.distanceModel) {
                    DistanceModel.INVERSE -> "inverse"
                    DistanceModel.LINEAR -> "linear"
                    DistanceModel.EXPONENTIAL -> "exponential"
                }
            panner.refDistance = attenuation.refDistance.toDouble()
            panner.maxDistance = attenuation.maxDistance.toDouble()
            panner.rolloffFactor = attenuation.rolloff.toDouble()
            panner.setPosition(x.toDouble(), y.toDouble(), z.toDouble())
            if (!spatial) {
                source.disconnect()
                gain.disconnect()
                source.connect(gain)
                gain.connect(panner)
                panner.connect(context.destination)
                spatial = true
            }
        }

        fun unspatialize() {
            if (!spatial) return
            val gain = gain ?: return
            gain.disconnect()
            gain.connect(WebAudio.obtainAudioContext().destination)
            spatial = false
        }

        fun setPosition(x: Float, y: Float, z: Float) {
            if (spatial) panner?.setPosition(x.toDouble(), y.toDouble(), z.toDouble())
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