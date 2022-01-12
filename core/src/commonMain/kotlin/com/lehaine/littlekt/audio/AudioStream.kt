package com.lehaine.littlekt.audio

import com.lehaine.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
interface AudioStream : Disposable {
    var volume: Float
    val playing: Boolean

    fun play(volume: Float = this.volume, loop: Boolean = false)

    fun stop()

    fun resume()

    fun pause()
}