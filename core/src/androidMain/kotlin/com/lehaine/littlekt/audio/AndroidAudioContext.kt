package com.lehaine.littlekt.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.lehaine.littlekt.Disposable

/**
 * @author Colt Daily
 * @date 2/13/22
 */
class AndroidAudioContext(private val androidCtx: Context) : Disposable {

    val audioManager: AudioManager = androidCtx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(16)
        .build()

    private fun createMediaPlayer() = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
        )
    }

    fun resume() {
        soundPool.autoResume()
    }

    fun pause() {
        soundPool.autoPause()
    }

    override fun dispose() {
        soundPool.release()
    }
}