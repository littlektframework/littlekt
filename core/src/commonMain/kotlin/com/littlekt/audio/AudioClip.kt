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
     * Play this audio clip. It isn't affected by the [AudioListener].
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param loop whether to loop this audio.
     * @return the id of this play, for [setVolume], [stop] & [isPlaying], or `-1` if it couldn't
     *   play
     */
    fun play(volume: Float = this.volume, loop: Boolean = false): Long

    /**
     * Plays this audio clip at [x], [y], [z] in world space, heard from the [Audio.listener]. Only
     * mono clips are positioned on desktop; stereo clips play without it.
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param loop whether to loop this audio.
     * @param attenuation how the sound gets quieter with distance
     * @return the id of this play, for [setPosition], [setVolume], [stop] & [isPlaying], or `-1` if
     *   it couldn't play
     */
    fun playAt(
        x: Float,
        y: Float,
        z: Float,
        volume: Float = this.volume,
        loop: Boolean = false,
        attenuation: AudioAttenuation = AudioAttenuation.DEFAULT,
    ): Long

    /** Moves the play with [id] from [playAt] to [x], [y], [z]. Does nothing once it has stopped. */
    fun setPosition(id: Long, x: Float, y: Float, z: Float)

    /** Sets the volume of the play with [id]. Does nothing once it has stopped. */
    fun setVolume(id: Long, volume: Float)

    /** Stops the play with [id]. */
    fun stop(id: Long)

    /** @return `true` while the play with [id] is playing */
    fun isPlaying(id: Long): Boolean

    /** Stops every play of this clip. */
    fun stop()

    /** Resumes the current audio from the position it was paused in. */
    fun resume()

    /** Pause the current audio clip. */
    fun pause()
}
