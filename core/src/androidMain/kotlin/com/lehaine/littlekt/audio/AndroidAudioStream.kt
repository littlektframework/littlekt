package com.lehaine.littlekt.audio

import android.media.MediaPlayer
import android.util.Log
import com.lehaine.littlekt.util.internal.lock

/**
 * @author Colt Daily
 * @date 2/13/22
 */
class AndroidAudioStream(private val audioContext: AndroidAudioContext, private val player: MediaPlayer) : AudioStream {
    override var volume: Float = 1f
        set(value) {
            field = value
            player.setVolume(value, value)
        }
    override var looping: Boolean
        get() = player.isLooping
        set(value) {
            player.isLooping = value
        }
    override val playing: Boolean
        get() = player.isPlaying

    internal var wasPlaying = false

    private var isPrepared = true

    override suspend fun play(volume: Float, loop: Boolean) {
        this.volume = volume
        this.looping = loop
        resume()
    }

    override fun stop() {
        player.stop()
        isPrepared = false
    }

    override fun resume() {
        if (!isPrepared) {
            player.prepare()
            isPrepared = true
        }
        player.start()
    }

    override fun pause() {
        if (playing) {
            player.pause()
        }
        wasPlaying = false
    }

    override fun dispose() {
        try {
            player.release()
        } catch (throwable: Throwable) {
            Log.w("AndroidAudioStream", "Error while disposing of AndroidAudioStream instance.")
        } finally {
            audioContext.disposeStream(this)
        }
    }
}