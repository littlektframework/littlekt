package com.littlekt.audio

import kotlin.js.JsAny
import kotlin.js.Promise
import org.w3c.dom.HTMLMediaElement

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
external class AudioContext {
    val destination: AudioNode

    val listener: JsAudioListener

    val state: String

    fun resume(): Promise<JsAny?>

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
