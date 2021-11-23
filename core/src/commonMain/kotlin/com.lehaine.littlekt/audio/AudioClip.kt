package com.lehaine.littlekt.audio

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
expect class AudioClip {

    var volume: Float
    var currentTime: Float
    val duration: Float
    val isEnded: Boolean
    var loop: Boolean
    var minIntervalMs: Float

    fun play()
    fun stop()

}