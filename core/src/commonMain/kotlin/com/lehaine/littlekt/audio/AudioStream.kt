package com.lehaine.littlekt.audio

import com.lehaine.littlekt.Disposable
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
interface AudioStream : Disposable {
    var volume: Float
    val duration: Duration

    fun play(volume: Float = this.volume, loop: Boolean = false)

    fun stop()

    fun resume()

    fun pause()
}