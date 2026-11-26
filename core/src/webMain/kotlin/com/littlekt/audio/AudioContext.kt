package com.littlekt.audio

import kotlin.js.JsAny
import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.MessagePort

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
external class AudioContext() {
    constructor(options: AudioContextOptions)

    val destination: AudioNode

    val listener: JsAudioListener

    val state: String

    val sampleRate: Double

    val audioWorklet: AudioWorklet?

    fun resume(): Promise<JsAny?>

    fun close(): Promise<JsAny?>

    fun createScriptProcessor(
        bufferSize: Int,
        numberOfInputChannels: Int,
        numberOfOutputChannels: Int,
    ): ScriptProcessorNode

    fun createMediaElementSource(element: HTMLMediaElement): AudioNode

    fun createPanner(): PannerNode

    fun createGain(): GainNode
}

open external class AudioNode {
    fun connect(destination: AudioNode): AudioNode

    fun disconnect()
}

external class PannerNode : AudioNode {
    var panningModel: String
    var distanceModel: String
    var refDistance: Double
    var maxDistance: Double
    var rolloffFactor: Double

    fun setPosition(x: Double, y: Double, z: Double)
}

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class AudioParam {
    var value: Double
}

external class JsAudioListener {
    fun setPosition(x: Double, y: Double, z: Double)

    fun setOrientation(x: Double, y: Double, z: Double, xUp: Double, yUp: Double, zUp: Double)
}

external interface AudioContextOptions : JsAny {
    var sampleRate: Int
}

external class OfflineAudioContext(numberOfChannels: Int, length: Int, sampleRate: Int) {
    fun decodeAudioData(audioData: ArrayBuffer): Promise<AudioBuffer>
}

external class AudioBuffer : JsAny {
    val numberOfChannels: Int
    val length: Int
    val sampleRate: Double

    fun getChannelData(channel: Int): Float32Array
}

external class AudioWorklet {
    fun addModule(moduleURL: String): Promise<JsAny?>
}

external class AudioWorkletNode(context: AudioContext, name: String, options: JsAny) : AudioNode {
    val port: MessagePort
}

external class ScriptProcessorNode : AudioNode {
    var onaudioprocess: ((AudioProcessingEvent) -> Unit)?
}

external class AudioProcessingEvent {
    val outputBuffer: AudioBuffer
}
