package com.littlekt.examples.audio

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.audio.PcmSource
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readAudioSamples
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.input.Key
import com.littlekt.renderFrame
import com.littlekt.resources.Fonts
import com.littlekt.surfaceColor
import com.littlekt.util.viewport.ScreenViewport
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.sin

/**
 * An example of mixing audio sample by sample through a [com.littlekt.audio.PcmOutput]. A
 * synthesized tone plays under a decoded WAV file that is mixed in on space.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
class PcmAudioExample(context: Context) : ContextListener(context) {

    private class Mixer(private val sampleRate: Int, private val wave: FloatArray, private val waveChannels: Int, waveRate: Int) :
        PcmSource {
        @Volatile var toneOn = true
        @Volatile var frequency = 220f
        @Volatile var trigger = false

        private val waveStep = waveRate.toDouble() / sampleRate
        private val waveFrames = wave.size / waveChannels
        private var phase = 0.0
        private var wavePosition = -1.0

        override fun fill(buffer: FloatArray, frames: Int) {
            if (trigger) {
                trigger = false
                wavePosition = 0.0
            }
            val step = 2.0 * PI * frequency / sampleRate
            for (f in 0 until frames) {
                var left = 0f
                var right = 0f
                if (toneOn) {
                    val tone = sin(phase).toFloat() * 0.2f
                    left += tone
                    right += tone
                }
                phase += step
                if (phase > 2.0 * PI) phase -= 2.0 * PI
                if (wavePosition >= 0.0) {
                    val frame = wavePosition.toInt()
                    if (frame >= waveFrames) {
                        wavePosition = -1.0
                    } else {
                        val i = frame * waveChannels
                        left += wave[i]
                        right += wave[i + waveChannels - 1]
                        wavePosition += waveStep
                    }
                }
                buffer[f * 2] = left.coerceIn(-1f, 1f)
                buffer[f * 2 + 1] = right.coerceIn(-1f, 1f)
            }
        }
    }

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()

        val wave = resourcesVfs["random.wav"].readAudioSamples()
        val mixer = Mixer(SAMPLE_RATE, wave.samples, wave.channels, wave.sampleRate)
        val output = audio.createPcmOutput(sampleRate = SAMPLE_RATE, channels = 2, source = mixer)
        output.play()

        val batch = SpriteBatch(graphics.device, graphics, graphics.preferredFormat)
        val viewport = ScreenViewport(graphics.width, graphics.height)
        viewport.update(graphics.width, graphics.height, true)
        val text = TextureFontCache(Fonts.default)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        fun updateText() {
            text.setText(
                "Space: mix in random.wav | T: tone ${if (mixer.toneOn) "on" else "off"} | " +
                    "Up/Down: ${mixer.frequency.toInt()} Hz | P: ${if (output.playing) "pause" else "play"} | " +
                    "latency ${output.latency.inWholeMilliseconds} ms",
                16f,
                viewport.virtualHeight - 32f,
            )
        }
        updateText()

        onResize { width, height ->
            viewport.update(width, height, true)
            updateText()
        }

        onUpdate {
            if (input.isKeyJustPressed(Key.SPACE)) mixer.trigger = true
            if (input.isKeyJustPressed(Key.T)) {
                mixer.toneOn = !mixer.toneOn
                updateText()
            }
            if (input.isKeyJustPressed(Key.ARROW_UP)) {
                mixer.frequency = (mixer.frequency * 1.25f).coerceAtMost(2000f)
                updateText()
            }
            if (input.isKeyJustPressed(Key.ARROW_DOWN)) {
                mixer.frequency = (mixer.frequency / 1.25f).coerceAtLeast(55f)
                updateText()
            }
            if (input.isKeyJustPressed(Key.P)) {
                if (output.playing) output.pause() else output.play()
                updateText()
            }

            renderFrame("PCM Audio Command Encoder") { frame, commandEncoder ->
                val pass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor), label = "PCM Audio Pass")
                    )
                viewport.camera.update()
                batch.begin(viewport.camera.viewProjection)
                text.draw(batch)
                batch.flush(pass)
                batch.end()
                pass.end()
                pass.release()
            }
        }

        onRelease {
            output.release()
            batch.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44100
    }
}
