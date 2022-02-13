package com.lehaine.littlekt.audio

import android.media.AudioManager
import android.media.SoundPool
import kotlin.time.Duration

/**
 * @author Colt Daily
 * @date 2/13/22
 */
class AndroidAudioClip(val manager: AudioManager, val soundPool: SoundPool, val soundId: Int) : AudioClip {

    override var volume: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    override val duration: Duration
        get() = TODO("Not yet implemented")

    override fun play(volume: Float, loop: Boolean) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}