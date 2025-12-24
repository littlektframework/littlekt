package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.GameButton
import com.littlekt.resources.Fonts
import com.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 4/17/2024
 */
class InputExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        onResize { width, height ->
            viewport.update(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        var buttonText = ""
        var axisText = ""
        var triggerText = ""
        input.inputProcessor {
            onGamepadTriggerChanged { button, pressure, gamepad ->
                triggerText = "Trigger $button pressed ($pressure) on gamepad '$gamepad'"
            }
            onGamepadButtonPressed { button, pressure, gamepad ->
                buttonText = "Button $button pressed (${pressure}) on gamepad '$gamepad'"
            }
            onGamepadJoystickMoved { stick, xAxis, yAxis, gamepad ->
                axisText = "Stick $stick moved ($xAxis,$yAxis) on gamepad '$gamepad'"
            }
        }
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }

                TextureStatus.TIMEOUT,
                TextureStatus.OUTDATED,
                TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }

                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                    return@onUpdate
                }
            }
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor =
                                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                                        else Color.DARK_GRAY,
                                )
                            )
                        )
                )
            camera.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                Fonts.default.draw(it, buttonText, 0f, 0f, align = HAlign.CENTER)
                Fonts.default.draw(it, axisText, 0f, -25f, align = HAlign.CENTER)
                Fonts.default.draw(it, triggerText, 0f, -50f, align = HAlign.CENTER)
            }

            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease { batch.release() }
    }
}
