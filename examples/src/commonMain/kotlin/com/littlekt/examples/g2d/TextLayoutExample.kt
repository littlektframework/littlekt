package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readTexture
import com.littlekt.file.vfs.readTtfFont
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import com.littlekt.input.PointerType
import com.littlekt.renderFrame
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScreenViewport
import kotlin.math.sin

/**
 * An example of letter spacing, line spacing & justified text, and of [SpriteBatch] scissor rects
 * that change within a single flush. The logo is decoded from bytes and the render pass descriptor
 * is reused each frame. Click or touch to show the pointer type.
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
class TextLayoutExample(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val ttf = resourcesVfs["LiberationSans-Regular.ttf"].readTtfFont()
        val font = ttf.raster(device, 18f)
        val logo = resourcesVfs["logo.png"].readBytes().readTexture(device, graphics.textureFormat)
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ScreenViewport(0, 0, graphics.width, graphics.height)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)
        val panelColor = Color(0.15f, 0.15f, 0.2f, 1f)
        val boxColor = Color(1f, 1f, 1f, 0.12f)
        val paragraph =
            "Scissor rects change between these panels without flushing the batch. Each panel " +
                "is drawn with its own clip in the same render pass. "
        val panels = List(3) { TextureFontCache(font) }
        val spacings = listOf(0f, 3f, 8f)
        val spacingCaches = List(3) { TextureFontCache(font) }
        val rightCache = TextureFontCache(font)
        val justifyCache = TextureFontCache(font)
        val pointerCache = TextureFontCache(font)
        val panelWidth = 260f
        val panelHeight = 120f
        var pointerType = PointerType.MOUSE
        var lastShownType: PointerType? = null
        var passDescriptor: RenderPassDescriptor? = null
        var time = 0f

        input.addInputProcessor(
            object : InputProcessor {
                override fun touchDown(
                    screenX: Float,
                    screenY: Float,
                    pointer: Pointer,
                    type: PointerType,
                ): Boolean {
                    pointerType = type
                    return false
                }
            }
        )

        fun panelX(i: Int) = 30f + i * (panelWidth + 30f)

        fun panelY() = graphics.height - 30f - panelHeight

        fun layout() {
            panels.forEachIndexed { i, cache ->
                cache.setText(
                    paragraph + paragraph,
                    0f,
                    0f,
                    targetWidth = panelWidth - 20f,
                    wrap = true,
                    letterSpacing = i.toFloat(),
                    lineSpacing = i * 3f,
                )
            }
            var y = panelY() - 50f
            spacings.forEachIndexed { i, spacing ->
                spacingCaches[i].setText(
                    "letterSpacing = ${spacing.toInt()}",
                    30f,
                    y,
                    targetWidth = 300f,
                    align = HAlign.CENTER,
                    letterSpacing = spacing,
                )
                y -= 34f
            }
            rightCache.setText(
                "Right aligned\nwith spacing",
                30f,
                y,
                targetWidth = 300f,
                align = HAlign.RIGHT,
                letterSpacing = 4f,
                lineSpacing = 6f,
            )
            justifyCache.setText(
                "Justified text stretches every wrapped line to the full width by widening its " +
                    "spaces, while the last line keeps its alignment. Line spacing adds room " +
                    "between the lines.",
                380f,
                panelY() - 50f,
                targetWidth = 420f,
                wrap = true,
                lineSpacing = 6f,
                justify = true,
            )
        }

        onResize { width, height ->
            viewport.update(width, height, true)
            layout()
        }

        fun scissor(x: Float, y: Float, width: Float, height: Float) {
            val sx = graphics.backBufferWidth.toFloat() / graphics.width
            val sy = graphics.backBufferHeight.toFloat() / graphics.height
            val left = (x * sx).toInt().coerceIn(0, graphics.backBufferWidth)
            val top = ((graphics.height - y - height) * sy).toInt().coerceIn(0, graphics.backBufferHeight)
            val right = ((x + width) * sx).toInt().coerceIn(left, graphics.backBufferWidth)
            val bottom = ((graphics.height - y) * sy).toInt().coerceIn(top, graphics.backBufferHeight)
            batch.setScissorRect(left, top, right - left, bottom - top)
        }

        onUpdate { dt ->
            time += dt.seconds
            if (pointerType != lastShownType) {
                lastShownType = pointerType
                pointerCache.setText("Last pointer: $pointerType", 380f, 40f)
            }
            renderFrame { frame, commandEncoder ->
                val descriptor =
                    passDescriptor
                        ?: RenderPassDescriptor(colorAttachment(frame, clearColor)).also {
                            passDescriptor = it
                        }
                descriptor.colorAttachments[0].view = frame
                val renderPass = commandEncoder.beginRenderPass(descriptor)
                viewport.camera.update()
                batch.use(renderPass, viewport.camera.viewProjection) {
                    val py = panelY()
                    for (i in panels.indices) {
                        val x = panelX(i)
                        scissor(0f, 0f, graphics.width.toFloat(), graphics.height.toFloat())
                        it.draw(Textures.white, x, py, width = panelWidth, height = panelHeight, color = panelColor)
                        scissor(x, py, panelWidth, panelHeight)
                        val scroll = (time * 20f + i * 40f) % 200f
                        panels[i].setPosition(x + 10f, py + panelHeight - 20f + scroll)
                        panels[i].draw(it)
                        val bounce = sin(time * 2f + i) * 60f
                        val logoHeight = 96f * logo.height / logo.width
                        it.draw(logo, x + panelWidth * 0.5f - 48f + bounce, py + 10f, width = 96f, height = logoHeight)
                    }
                    scissor(0f, 0f, graphics.width.toFloat(), graphics.height.toFloat())
                    var y = py - 58f
                    repeat(spacingCaches.size) { _ ->
                        it.draw(Textures.white, 30f, y, width = 300f, height = 28f, color = boxColor)
                        y -= 34f
                    }
                    it.draw(Textures.white, 30f, y - 27f, width = 300f, height = 55f, color = boxColor)
                    it.draw(Textures.white, 380f, py - 140f, width = 420f, height = 112f, color = boxColor)
                    spacingCaches.forEach { cache -> cache.draw(it) }
                    rightCache.draw(it)
                    justifyCache.draw(it)
                    pointerCache.draw(it)
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            logo.release()
            ttf.release()
        }
    }
}
