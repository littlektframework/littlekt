package com.littlekt.audio

import kotlin.js.unsafeCast
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import org.w3c.dom.MessageEvent

/**
 * A [PcmOutput] on the web. An `AudioWorklet` plays the buffers, which bounce between the worklet &
 * the main thread, where the [PcmSource] writes them. Falls back to a `ScriptProcessorNode` when
 * worklets aren't available, such as on insecure pages.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
class WebPcmOutput
internal constructor(
    sampleRate: Int,
    channels: Int,
    bufferFrames: Int,
    bufferCount: Int,
    source: PcmSource,
) : PcmOutput {
    private val filler = PcmFiller(sampleRate, channels, bufferFrames, bufferCount, source)
    private val free = ArrayList<Float32Array>(bufferCount)
    private var gain: GainNode? = null
    private var worklet: AudioWorkletNode? = null
    private var processor: ScriptProcessorNode? = null
    private var processorFrame = bufferFrames
    private var isPlaying = false
    private var released = false

    private val volumeListener: () -> Unit = { applyVolume() }

    private val onMessage: (MessageEvent) -> Unit = { event ->
        val buffer = event.data!!.unsafeCast<Float32Array>()
        val node = worklet
        if (isPlaying && node != null) {
            write(buffer)
            postPcmBuffer(node.port, buffer)
        } else {
            free += buffer
        }
    }

    private val onProcess: (AudioProcessingEvent) -> Unit = { event -> process(event.outputBuffer) }

    override val sampleRate: Int = sampleRate
    override val channels: Int = channels
    override val bufferFrames: Int = bufferFrames
    override val bufferCount: Int = bufferCount

    override var latency: Duration = filler.latency
        private set

    override val framesWritten: Long
        get() = filler.framesWritten

    override val playing: Boolean
        get() = isPlaying

    override var volume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyVolume()
        }

    init {
        WebAudio.addVolumeListener(volumeListener)
    }

    /** Connects this output once [context] can play it, with a worklet if [useWorklet]. */
    internal fun start(context: AudioContext, useWorklet: Boolean) {
        if (released) return
        val gain = context.createGain().also { gain = it }
        gain.connect(context.destination)
        if (useWorklet) {
            val node = AudioWorkletNode(context, PCM_PROCESSOR, pcmWorkletOptions(channels))
            node.port.onmessage = onMessage
            node.connect(gain)
            worklet = node
            for (i in 0 until bufferCount) free += Float32Array(bufferFrames * channels)
        } else {
            var size = MIN_PROCESSOR_SIZE
            while (size < bufferFrames && size < MAX_PROCESSOR_SIZE) size *= 2
            val node = context.createScriptProcessor(size, 0, channels)
            node.onaudioprocess = onProcess
            node.connect(gain)
            processor = node
            processorFrame = bufferFrames
            latency = (size * 2.0 / sampleRate).seconds
        }
        applyVolume()
        if (isPlaying) {
            isPlaying = false
            play()
        }
    }

    override fun play() {
        if (released || isPlaying) return
        isPlaying = true
        val node = worklet ?: return
        postPcmCommand(node.port, PCM_PLAY)
        while (free.isNotEmpty()) {
            val buffer = free.removeAt(free.size - 1)
            write(buffer)
            postPcmBuffer(node.port, buffer)
        }
    }

    override fun pause() {
        if (!isPlaying) return
        isPlaying = false
        worklet?.let { postPcmCommand(it.port, PCM_PAUSE) }
    }

    override fun stop() {
        isPlaying = false
        processorFrame = bufferFrames
        worklet?.let { postPcmCommand(it.port, PCM_STOP) }
    }

    override fun release() {
        if (released) return
        stop()
        released = true
        worklet?.let {
            postPcmCommand(it.port, PCM_RELEASE)
            it.port.onmessage = null
            it.disconnect()
        }
        processor?.let {
            it.onaudioprocess = null
            it.disconnect()
        }
        gain?.disconnect()
        worklet = null
        processor = null
        gain = null
        free.clear()
        WebAudio.removeVolumeListener(volumeListener)
        WebAudio.removePcmOutput(this)
    }

    private fun applyVolume() {
        gain?.gain?.value = (volume * WebAudio.masterVolume).toDouble()
    }

    private fun write(buffer: Float32Array) {
        val samples = filler.fill()
        for (i in samples.indices) buffer[i] = samples[i]
    }

    private fun process(output: AudioBuffer) {
        val frames = output.length
        val left = output.getChannelData(0)
        val right = if (channels == 2) output.getChannelData(1) else left
        if (!isPlaying) {
            for (f in 0 until frames) {
                left[f] = 0f
                right[f] = 0f
            }
            return
        }
        val samples = filler.buffer
        var frame = processorFrame
        for (f in 0 until frames) {
            if (frame == bufferFrames) {
                filler.fill()
                frame = 0
            }
            val i = frame * channels
            left[f] = samples[i]
            if (channels == 2) right[f] = samples[i + 1]
            frame++
        }
        processorFrame = frame
    }

    private companion object {
        const val MIN_PROCESSOR_SIZE = 256
        const val MAX_PROCESSOR_SIZE = 16384
    }
}
