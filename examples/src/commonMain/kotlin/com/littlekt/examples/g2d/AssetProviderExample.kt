package com.littlekt.examples.g2d

import com.littlekt.examples.*
import com.littlekt.AssetProvider
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.ldtk.LDtkMapLoader
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkWorld
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.resources.Fonts
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example of loading fonts and an LDtk map asynchronously with an [AssetProvider] and drawing
 * them once everything has loaded. Move the camera with WASD.
 *
 * @author Colton Daily
 * @date 7/7/2024
 */
class AssetProviderExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val assets = AssetProvider(this)

        val pixelFont: BitmapFont by assets.load(resourcesVfs["m5x7_32.fnt"])
        val arialFont: BitmapFont by assets.load(resourcesVfs["arial_32.fnt"])
        val mapLoader: LDtkMapLoader by assets.load(resourcesVfs["ldtk/world-1.5.3.ldtk"])
        val world: LDtkWorld by assets.prepare { mapLoader.loadMap() }

        val batch = SpriteBatch(graphics.device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height -> viewport.update(width, height) }

        addWASDMovement(camera, 0.05f)

        onUpdate {
            if (!assets.fullyLoaded) {
                assets.update()
                return@onUpdate
            }

            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, clearColor)))
                    )
                camera.update()

                batch.use(renderPass, camera.viewProjection) {
                    world.levels[0].render(it, camera, 0f, 0f, 1f)
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
            if (assets.fullyLoaded) {
                pixelFont.release()
                arialFont.release()
                mapLoader.release()
            }
        }
    }
}
