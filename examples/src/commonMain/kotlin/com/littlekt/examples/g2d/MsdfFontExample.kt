package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readTtfFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.MsdfStyle
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.geom.radians
import com.littlekt.renderFrame
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScreenViewport
import kotlin.math.sin

/**
 * An example of drawing a TrueType font from multi-channel signed distance fields. One set of glyph
 * fields draws every size, scale and rotation sharply, with outlines, glows and weights.
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
class MsdfFontExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val ttf = resourcesVfs["LiberationSans-Regular.ttf"].readTtfFont()
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ScreenViewport(0, 0, graphics.width, graphics.height)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        val font = ttf.msdf(device, 32f, distanceRange = 12f)
        val sizes = listOf(12f, 16f, 24f, 40f, 72f)
        val sizeCaches = sizes.map { TextureFontCache(font.withSize(it)) }
        val styles =
            listOf(
                "Outline" to MsdfStyle(outlineWidth = 0.05f, outlineColor = Color.BLACK),
                "Glow" to MsdfStyle(glowWidth = 0.1f, glowColor = Color(0.2f, 0.6f, 1f, 0.9f)),
                "Bold" to MsdfStyle(weight = 0.04f),
                "Thin" to MsdfStyle(weight = -0.03f),
                "Both" to
                    MsdfStyle(
                        outlineWidth = 0.04f,
                        outlineColor = Color(0.6f, 0.1f, 0.1f, 1f),
                        glowWidth = 0.07f,
                        glowColor = Color(1f, 0.8f, 0.2f, 0.8f),
                    ),
            )
        val styleCaches = styles.map { (_, style) -> TextureFontCache(font.withSize(40f).withStyle(style)) }
        val spinCache = TextureFontCache(font.withStyle(styles[0].second))
        var time = 0f

        fun layout() {
            var y = graphics.height - 20f
            sizes.forEachIndexed { i, size ->
                y -= size * 1.2f
                sizeCaches[i].setText("${size.toInt()}px The quick brown fox jumps over the lazy dog", 20f, y)
            }
            y -= 60f
            var x = 20f
            styles.forEachIndexed { i, (name, _) ->
                styleCaches[i].setText(name, x, y)
                x += 180f
            }
        }

        onResize { width, height ->
            viewport.update(width, height, true)
            layout()
        }

        onUpdate { dt ->
            time += dt.seconds
            val scale = 1.5f + sin(time) * 1.2f
            spinCache.setText(
                "Scale & rotate",
                graphics.width * 0.5f,
                160f,
                scaleX = scale,
                scaleY = scale,
                rotation = (time * 0.5f).radians,
                color = Color.YELLOW,
            )
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                viewport.camera.update()
                batch.use(renderPass, viewport.camera.viewProjection) {
                    sizeCaches.forEach { cache -> cache.draw(it) }
                    styleCaches.forEach { cache -> cache.draw(it) }
                    spinCache.draw(it)
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            ttf.release()
        }
    }
}
