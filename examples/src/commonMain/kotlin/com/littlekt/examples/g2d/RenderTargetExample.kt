package com.littlekt.examples.g2d

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*

/**
 * An example of rendering to a texture and then drawing that texture to the surface.
 *
 * @author Colton Daily
 * @date 5/9/2024
 */
class RenderTargetExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val device = graphics.device
        val target = EmptyTexture(device, graphics.preferredFormat, 256, 256)
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val targetClearColor = graphics.surfaceColor(Color.YELLOW)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onUpdate {
            renderFrame("render target command encoder") { frame, commandEncoder ->
                val renderTargetRenderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(target.view, targetClearColor)),
                            label = "Render target render pass",
                        )
                    )

                batch.begin()
                batch.draw(icon, 0f, 0f)
                batch.flush(renderTargetRenderPass)
                renderTargetRenderPass.end()
                renderTargetRenderPass.release()

                val surfaceRenderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(frame, clearColor)),
                            label = "Surface render pass",
                        )
                    )
                batch.draw(
                    target,
                    0f,
                    0f,
                    originX = target.width * 0.5f,
                    originY = target.height * 0.5f,
                )
                batch.flush(surfaceRenderPass)
                batch.end()
                surfaceRenderPass.end()
                surfaceRenderPass.release()
            }
        }

        onRelease {
            batch.release()
            target.release()
            icon.release()
        }
    }
}
