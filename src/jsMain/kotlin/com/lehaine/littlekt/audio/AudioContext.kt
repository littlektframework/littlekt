package com.lehaine.littlekt.audio

import org.khronos.webgl.ArrayBuffer

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
external class JsSound

external class JsSoundDestination

external class JsBufferSource {
    var buffer: JsSound

    fun connect(destination: JsSoundDestination)

    fun start(delay: Int)
}

external class AudioContext {

    val destination: JsSoundDestination

    fun decodeAudioData(bytes: ArrayBuffer, onLoad: (buffer: JsSound) -> Unit)

    fun createBufferSource(): JsBufferSource
}