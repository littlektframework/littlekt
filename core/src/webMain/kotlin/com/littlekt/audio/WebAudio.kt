package com.littlekt.audio

/**
 * The [Audio] output on the web. The browser picks the output device, so only [masterVolume] can
 * be changed.
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

    internal fun addVolumeListener(listener: () -> Unit) {
        listeners += listener
    }

    internal fun removeVolumeListener(listener: () -> Unit) {
        listeners -= listener
    }
}
