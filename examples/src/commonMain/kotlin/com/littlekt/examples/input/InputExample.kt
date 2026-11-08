package com.littlekt.examples.input

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.resources.Fonts
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example that displays the last gamepad button, trigger, and joystick input.
 *
 * @author Colton Daily
 * @date 4/17/2024
 */
class InputExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val batch = SpriteBatch(graphics.device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height -> viewport.update(width, height) }

        var buttonText = ""
        var axisText = ""
        var triggerText = ""
        input.inputProcessor {
            onGamepadTriggerChanged { button, pressure, gamepad ->
                triggerText = "Trigger $button pressed ($pressure) on gamepad '$gamepad'"
            }
            onGamepadButtonPressed { button, pressure, gamepad ->
                buttonText = "Button $button pressed ($pressure) on gamepad '$gamepad'"
            }
            onGamepadJoystickMoved { stick, xAxis, yAxis, gamepad ->
                axisText = "Stick $stick moved ($xAxis,$yAxis) on gamepad '$gamepad'"
            }
        }
        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, clearColor)))
                    )
                camera.update()

                batch.use(renderPass, camera.viewProjection) {
                    Fonts.default.draw(it, buttonText, 0f, 0f, align = HAlign.CENTER)
                    Fonts.default.draw(it, axisText, 0f, -25f, align = HAlign.CENTER)
                    Fonts.default.draw(it, triggerText, 0f, -50f, align = HAlign.CENTER)
                }

                renderPass.end()
                renderPass.release()
            }
        }

        onRelease { batch.release() }
    }
}
