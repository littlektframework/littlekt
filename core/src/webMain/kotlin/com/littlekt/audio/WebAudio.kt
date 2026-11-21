package com.littlekt.audio

/**
 * The [Audio] output on the web. The browser picks the output device, so only [masterVolume] can
 * be changed. Spatial sounds go through a shared Web Audio context created on the first
 * [AudioClip.playAt].
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
object WebAudio : Audio {
    private val listeners = mutableListOf<() -> Unit>()

    override var masterVolume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            for (i in listeners.indices) listeners[i]()
        }

    private val listenerPosition = FloatArray(3)
    private val listenerOrientation = floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f)
    private var audioContext: AudioContext? = null

    override val listener: AudioListener =
        object : AudioListener {
            override fun setPosition(x: Float, y: Float, z: Float) {
                listenerPosition[0] = x
                listenerPosition[1] = y
                listenerPosition[2] = z
                audioContext?.let { applyListenerPosition(it) }
            }

            override fun setOrientation(
                forwardX: Float,
                forwardY: Float,
                forwardZ: Float,
                upX: Float,
                upY: Float,
                upZ: Float,
            ) {
                listenerOrientation[0] = forwardX
                listenerOrientation[1] = forwardY
                listenerOrientation[2] = forwardZ
                listenerOrientation[3] = upX
                listenerOrientation[4] = upY
                listenerOrientation[5] = upZ
                audioContext?.let { applyListenerOrientation(it) }
            }
        }

    override val canSelectOutputDevice: Boolean = false

    override val outputDevices: List<String> = emptyList()

    override val outputDevice: String? = null

    override fun setOutputDevice(name: String?): Boolean = false

    /**
     * The maximum number of times each [AudioClip] can play at once. Once reached, the oldest play
     * of that clip restarts. Set from [com.littlekt.LittleKtProps.maxAudioClipInstances].
     */
    var maxInstancesPerClip: Int = DEFAULT_MAX_INSTANCES_PER_CLIP
        internal set

    /** The default for [maxInstancesPerClip]. */
    const val DEFAULT_MAX_INSTANCES_PER_CLIP = 8

    /** Gets the shared [AudioContext] for spatial sounds, creating and resuming it as needed. */
    internal fun obtainAudioContext(): AudioContext {
        val context = audioContext ?: AudioContext().also {
            audioContext = it
            applyListenerPosition(it)
            applyListenerOrientation(it)
        }
        if (context.state == "suspended") context.resume()
        return context
    }

    private fun applyListenerPosition(context: AudioContext) {
        context.listener.setPosition(
            listenerPosition[0].toDouble(),
            listenerPosition[1].toDouble(),
            listenerPosition[2].toDouble(),
        )
    }

    private fun applyListenerOrientation(context: AudioContext) {
        val o = listenerOrientation
        context.listener.setOrientation(
            o[0].toDouble(),
            o[1].toDouble(),
            o[2].toDouble(),
            o[3].toDouble(),
            o[4].toDouble(),
            o[5].toDouble(),
        )
    }

    internal fun addVolumeListener(listener: () -> Unit) {
        listeners += listener
    }

    internal fun removeVolumeListener(listener: () -> Unit) {
        listeners -= listener
    }
}
