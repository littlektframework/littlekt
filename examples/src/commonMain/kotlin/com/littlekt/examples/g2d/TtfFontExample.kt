package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readTtfFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.renderFrame
import com.littlekt.resources.Fonts
import com.littlekt.surfaceColor
import com.littlekt.util.viewport.ScreenViewport

/**
 * An example of drawing a TrueType font rasterized at exact pixel sizes, compared to the default
 * bitmap font. Each size is rasterized once and cached by the [com.littlekt.graphics.g2d.font.TtfFont].
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class TtfFontExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val ttf = resourcesVfs["LiberationSans-Regular.ttf"].readTtfFont()
        val serif = resourcesVfs["FreeSerif.ttf"].readTtfFont()
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ScreenViewport(0, 0, graphics.width, graphics.height)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)
        val sizes = listOf(11f, 14f, 18f, 24f, 36f, 56f)
        val caches = sizes.map { TextureFontCache(ttf.raster(device, it)) }
        val serifCache = TextureFontCache(serif.raster(device, 32f))
        val kerningCache = TextureFontCache(ttf.raster(device, 40f))
        val bitmapCache = TextureFontCache(Fonts.default)

        fun layout() {
            var y = graphics.height - 20f
            sizes.forEachIndexed { i, size ->
                val font = ttf.raster(device, size)
                y -= font.metrics.lineHeight
                caches[i].setText(
                    font,
                    "${size.toInt()}px LiberationSans: The quick brown fox jumps over the lazy dog 0123456789",
                    20f,
                    y,
                )
            }
            y -= 60f
            serifCache.setText(serif.raster(device, 32f), "FreeSerif 32px: Sphinx of black quartz", 20f, y)
            y -= 70f
            kerningCache.setText(ttf.raster(device, 40f), "Kerning: AVATAR To Wa Ty", 20f, y)
            y -= 60f
            bitmapCache.setText(Fonts.default, "Bitmap font: The quick brown fox jumps over the lazy dog", 20f, y)
        }

        onResize { width, height ->
            viewport.update(width, height, true)
            layout()
        }

        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                viewport.camera.update()
                batch.use(renderPass, viewport.camera.viewProjection) {
                    caches.forEachIndexed { i, cache -> cache.draw(it, ttf.raster(device, sizes[i]).textures) }
                    serifCache.draw(it, serif.raster(device, 32f).textures)
                    kerningCache.draw(it, ttf.raster(device, 40f).textures)
                    bitmapCache.draw(it, Fonts.default.textures)
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            ttf.release()
            serif.release()
        }
    }
}
