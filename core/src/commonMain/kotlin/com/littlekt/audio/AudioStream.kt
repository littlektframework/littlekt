package com.littlekt.audio

import com.littlekt.Releasable

/**
 * A stream of audio that can be played.
 *
 * @author Colton Daily
 * @date 12/19/2021
 */
interface AudioStream : Releasable {
    /**
     * Settings this will adjust the current volume of the stream.
     *
     * @return the current volume
     */
    var volume: Float

    /**
     * Setting this will allow it to loop without resetting.
     *
     * @return Whether or not this [AudioStream] is looping.
     */
    var looping: Boolean

    /** @return if the stream is currently playing */
    val playing: Boolean

    /**
     * Play this stream of audio.
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param loop whether to loop this audio. Defaults to the current [looping]
     */
    suspend fun play(volume: Float = this.volume, loop: Boolean = this.looping)

    /** Stops the current stream from playing and resets it to the beginning. */
    fun stop()

    /** Resumes the current stream for the current position. */
    fun resume()

    /** Pauses the stream at the current position. */
    fun pause()
}
