package com.littlekt.audio

import kotlin.js.JsAny
import org.khronos.webgl.Float32Array
import org.w3c.dom.MessagePort

internal fun audioContextOptions(sampleRate: Int): AudioContextOptions =
    js("({ sampleRate: sampleRate, latencyHint: 'interactive' })")

internal fun pcmWorkletOptions(channels: Int): JsAny =
    js(
        "({ numberOfInputs: 0, numberOfOutputs: 1, outputChannelCount: [channels], processorOptions: { channels: channels } })"
    )

internal fun scriptUrl(source: String): String =
    js("URL.createObjectURL(new Blob([source], { type: 'application/javascript' }))")

internal fun postPcmBuffer(port: MessagePort, buffer: Float32Array): Unit =
    js("port.postMessage(buffer, [buffer.buffer])")

internal fun postPcmCommand(port: MessagePort, command: Int): Unit = js("port.postMessage(command)")

internal const val PCM_PROCESSOR = "littlekt-pcm"

internal const val PCM_PLAY = 1
internal const val PCM_PAUSE = 0
internal const val PCM_STOP = 2
internal const val PCM_RELEASE = 3

internal const val PCM_PROCESSOR_SOURCE =
    """
class LittleKtPcmProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    this.channels = options.processorOptions.channels;
    this.queue = [];
    this.current = null;
    this.offset = 0;
    this.playing = false;
    this.released = false;
    this.port.onmessage = (e) => {
      const data = e.data;
      if (typeof data !== 'number') {
        this.queue.push(data);
      } else if (data === 2) {
        this.playing = false;
        if (this.current !== null) this.port.postMessage(this.current, [this.current.buffer]);
        this.current = null;
        while (this.queue.length > 0) {
          const buffer = this.queue.shift();
          this.port.postMessage(buffer, [buffer.buffer]);
        }
      } else if (data === 3) {
        this.released = true;
      } else {
        this.playing = data === 1;
      }
    };
  }

  process(inputs, outputs) {
    if (this.released) return false;
    if (!this.playing) return true;
    const out = outputs[0];
    const channels = this.channels;
    const frames = out[0].length;
    for (let f = 0; f < frames; f++) {
      if (this.current === null) {
        if (this.queue.length === 0) break;
        this.current = this.queue.shift();
        this.offset = 0;
      }
      const buffer = this.current;
      for (let c = 0; c < out.length; c++) out[c][f] = buffer[this.offset + Math.min(c, channels - 1)];
      this.offset += channels;
      if (this.offset >= buffer.length) {
        this.port.postMessage(buffer, [buffer.buffer]);
        this.current = null;
      }
    }
    return true;
  }
}
registerProcessor('littlekt-pcm', LittleKtPcmProcessor);
"""
