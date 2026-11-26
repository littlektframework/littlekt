package com.littlekt.audio

import kotlin.js.JsAny
import org.khronos.webgl.Float32Array
import org.w3c.dom.MessagePort

internal actual fun audioContextOptions(sampleRate: Int): AudioContextOptions =
    js("({ sampleRate: sampleRate, latencyHint: 'interactive' })")

internal actual fun pcmWorkletOptions(channels: Int): JsAny =
    js(
        "({ numberOfInputs: 0, numberOfOutputs: 1, outputChannelCount: [channels], processorOptions: { channels: channels } })"
    )

internal actual fun scriptUrl(source: String): String =
    js("URL.createObjectURL(new Blob([source], { type: 'application/javascript' }))")

internal actual fun postPcmBuffer(port: MessagePort, buffer: Float32Array): Unit =
    js("port.postMessage(buffer, [buffer.buffer])")

internal actual fun postPcmCommand(port: MessagePort, command: Int): Unit = js("port.postMessage(command)")

