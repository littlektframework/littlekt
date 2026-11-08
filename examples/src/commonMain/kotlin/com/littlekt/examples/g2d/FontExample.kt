package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.renderFrame
import com.littlekt.resources.Fonts
import com.littlekt.surfaceColor
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example of loading bitmap fonts and drawing text with different alignments. Move the camera
 * with WASD.
 *
 * @author Colton Daily
 * @date 4/17/2024
 */
class FontExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val pixelFont = resourcesVfs["m5x7_32.fnt"].readBitmapFont()
        val arialFont = resourcesVfs["arial_32.fnt"].readBitmapFont()
        val batch = SpriteBatch(graphics.device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height -> viewport.update(width, height) }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                camera.update()

                batch.use(renderPass, camera.viewProjection) {
                    pixelFont.draw(it, "Hello\nLittleKt!", 0f, 0f, align = HAlign.CENTER)
                    arialFont.draw(it, "Hello\nLittleKt!", -300f, 0f)
                    Fonts.default.draw(it, "Hello\nLittleKt!", 150f, 0f, align = HAlign.RIGHT)
                }

                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            pixelFont.release()
            arialFont.release()
        }
    }
}
