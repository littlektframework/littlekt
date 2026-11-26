package com.littlekt.audio

/**
 * The audio output of the application. Use [com.littlekt.Context.onAudioDevicesChanged] to know
 * when output devices are added or removed.
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
interface Audio {
    /** Multiplies the volume of every [AudioClip] & [AudioStream], from `0` to `1`. */
    var masterVolume: Float

    /** The listener that sounds played with [AudioClip.playAt] are heard from. */
    val listener: AudioListener

    /** `true` if [setOutputDevice] can switch the output device on this platform. */
    val canSelectOutputDevice: Boolean

    /** The names of the available output devices. Empty if they can't be listed. */
    val outputDevices: List<String>

    /** The name of the current output device, or `null` if unknown. */
    val outputDevice: String?

    /**
     * Switches the output to the device with [name], keeping everything that is playing.
     *
     * @param name a name from [outputDevices], or `null` to follow the system default device
     * @return `false` if switching isn't supported or failed
     */
    fun setOutputDevice(name: String?): Boolean

    /**
     * Creates a [PcmOutput] that plays the raw samples written by [source]. It starts paused; call
     * [PcmOutput.play] to start it.
     *
     * @param sampleRate the number of frames played per second
     * @param channels `1` for mono or `2` for stereo
     * @param bufferFrames the number of frames [source] writes per buffer
     * @param bufferCount the number of buffers queued for playback, at least `2`. The latency is
     *   `bufferFrames * bufferCount / sampleRate` seconds.
     * @param source writes the samples. Called from the audio thread on desktop.
     */
    fun createPcmOutput(
        sampleRate: Int = PcmOutput.DEFAULT_SAMPLE_RATE,
        channels: Int = 2,
        bufferFrames: Int = PcmOutput.DEFAULT_BUFFER_FRAMES,
        bufferCount: Int = PcmOutput.DEFAULT_BUFFER_COUNT,
        source: PcmSource,
    ): PcmOutput
}
