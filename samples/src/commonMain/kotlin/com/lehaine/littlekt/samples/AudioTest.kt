package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.input.Key
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class AudioTest(context: Context) : ContextListener(context) {

    private var loading = true
    private var init = false

    private lateinit var sfx: AudioClip
    private lateinit var music: AudioClip

    init {
        fileHandler.launch {
            sfx = loadAudioClip("random.wav")
            music = loadAudioClip("music.mp3")
            loading = false
        }
    }

    private fun init() {
        sfx.play()
        music.play()
    }

    override fun render(dt: Duration) {
        if (loading) return
        if (!loading && !init) {
            init()
            init = true
        }

        if (input.isKeyJustPressed(Key.ENTER)) {
            sfx.stop()
            sfx.play()
        }
    }
}