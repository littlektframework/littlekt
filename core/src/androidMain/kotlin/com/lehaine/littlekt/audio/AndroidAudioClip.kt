package com.lehaine.littlekt.audio

import android.media.SoundPool
import com.lehaine.littlekt.util.datastructure.IntArrayList
import kotlin.time.Duration

/**
 * @author Colt Daily
 * @date 2/13/22
 */
class AndroidAudioClip(private val soundPool: SoundPool, private val soundId: Int) : AudioClip {

    override var volume: Float = 1f
        set(value) {
            field = value
            streamIds.forEach {
                soundPool.setVolume(it, value, value)
            }
        }

    // we are not able to determine the duration of a clip using a [SoulPool]
    override val duration: Duration = Duration.ZERO

    private val streamIds = IntArrayList(8)

    override fun play(volume: Float, loop: Boolean) {
        if (streamIds.size == 8) streamIds.pop()
        val streamId = soundPool.play(soundId, volume, volume, 1, if (loop) -1 else 0, 1f)
        if (streamId == 0) return
        streamIds.insertAt(0, streamId)
    }

    override fun stop() {
        streamIds.forEach {
            soundPool.stop(it)
        }
    }

    override fun resume() {
        soundPool.autoResume()
    }

    override fun pause() {
        soundPool.autoPause()
    }

    override fun dispose() {
        soundPool.unload(soundId)
    }
}