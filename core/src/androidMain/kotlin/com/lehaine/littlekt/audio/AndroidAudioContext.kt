package com.lehaine.littlekt.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.internal.lock
import kotlinx.coroutines.launch

/**
 * @author Colt Daily
 * @date 2/13/22
 */
class AndroidAudioContext(private val androidCtx: Context) : Disposable {

    internal val streams = mutableListOf<AndroidAudioStream>()

    internal val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(16)
        .build()

    internal fun createMediaPlayer() = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
        )
    }

    fun resume() {
        lock(streams) {
            KtScope.launch {
                streams.fastForEach {
                    if (it.wasPlaying) {
                        it.play()
                    }
                }
            }
        }
        soundPool.autoResume()
    }

    fun pause() {
        lock(streams) {
            streams.fastForEach {
                if (it.playing) {
                    it.pause()
                    it.wasPlaying = true
                } else {
                    it.wasPlaying = false
                }
            }
        }
        soundPool.autoPause()
    }


    fun disposeStream(stream: AudioStream) {
        lock(streams) {
            streams.remove(stream)
        }
    }

    override fun dispose() {
        lock(streams) {
            streams.fastForEach {
                it.dispose()
            }
        }
        soundPool.release()
    }
}