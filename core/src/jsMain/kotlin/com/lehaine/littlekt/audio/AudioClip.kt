package com.lehaine.littlekt.audio

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
actual class AudioClip {
    actual var volume: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var currentTime: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val duration: Float
        get() = TODO("Not yet implemented")
    actual val isEnded: Boolean
        get() = TODO("Not yet implemented")
    actual var loop: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var minIntervalMs: Float
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun play() {
    }

    actual fun stop() {
    }


}