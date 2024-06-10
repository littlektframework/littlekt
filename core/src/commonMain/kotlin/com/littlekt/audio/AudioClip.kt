package com.littlekt.audio

import com.littlekt.Releasable
import kotlin.time.Duration

/**
 * A fully ready clip of audio that can be played.
 *
 * @author Colton Daily
 * @date 12/19/2021
 */
interface AudioClip : Releasable {
    /** The current volume of the audip clip. */
    var volume: Float

    /** The duration of audio. */
    val duration: Duration

    /**
     * Play this audio clip.
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param loop whether to loop this audio.
     */
    fun play(volume: Float = this.volume, loop: Boolean = false)

    /** Stops the audio from playing. */
    fun stop()

    /** Resumes the current audio from the position it was paused in. */
    fun resume()

    /** Pause the current audio clip. */
    fun pause()
}
