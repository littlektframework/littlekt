package com.lehaine.littlekt.io

import com.lehaine.littlekt.audio.AudioContext
import com.lehaine.littlekt.audio.JsBufferSource
import com.lehaine.littlekt.audio.JsSound

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebSound(private val sound: JsSound, private val audioContext: AudioContext) : Sound {

    override fun play(loop: Int) {
        val source: JsBufferSource = audioContext.createBufferSource()
        source.buffer = sound
        source.connect(audioContext.destination)
        source.start(0)
    }

}