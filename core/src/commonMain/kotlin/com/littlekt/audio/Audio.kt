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
}
