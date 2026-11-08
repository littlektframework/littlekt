package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.math.PI2_F
import com.littlekt.math.PI_F
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.renderFrame
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ExtendViewport
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of drawing an animated composition of lines, paths, rectangles, triangles, circles,
 * ellipses, and polygons with a [ShapeRenderer] on top of a stretched [NinePatch] panel.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class ShapesExample(context: Context) : ContextListener(context) {
    private val starPoints = FloatArray(20)
    private val wavePoints = FloatArray(WAVE_POINTS * 2)

    private fun updateStar(x: Float, y: Float, outer: Float, inner: Float, rotation: Float) {
        for (i in 0 until 10) {
            val angle = rotation + i * PI_F / 5f
            val radius = if (i % 2 == 0) outer else inner
            starPoints[i * 2] = x + cos(angle) * radius
            starPoints[i * 2 + 1] = y + sin(angle) * radius
        }
    }

    private fun updateWave(x: Float, y: Float, width: Float, time: Float) {
        for (i in 0 until WAVE_POINTS) {
            val px = x + width * i / (WAVE_POINTS - 1)
            wavePoints[i * 2] = px
            wavePoints[i * 2 + 1] = y + sin(px * 0.025f + time * 3f) * 16f
        }
    }

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val panelTexture = resourcesVfs["bg_9.png"].readTexture()
        val panel = NinePatch(panelTexture, left = 2, right = 2, top = 2, bottom = 4)
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val shapes = ShapeRenderer(batch)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.fromHex("1d2b53"))
        val panelColor = Color.fromHex("fff1e8")
        val ink = Color.fromHex("1d2b53")
        val red = Color.fromHex("ff004d")
        val orange = Color.fromHex("ffa300")
        val yellow = Color.fromHex("ffec27")
        val green = Color.fromHex("00e436")
        val blue = Color.fromHex("29adff")
        val purple = Color.fromHex("83769c")
        val pink = Color.fromHex("ff77a8")
        val palette = arrayOf(red, orange, yellow, green, blue, pink)
        val panelWidth = 820f
        val panelHeight = 440f
        var time = 0f

        onResize { width, height -> viewport.update(width, height, true) }

        onUpdate { dt ->
            time += dt.seconds
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                camera.update()
                batch.use(renderPass, camera.viewProjection) {
                    shapes.updatePixelSize(graphics.width)
                    val cx = viewport.virtualWidth * 0.5f
                    val cy = viewport.virtualHeight * 0.5f
                    val left = cx - panelWidth * 0.5f
                    val bottom = cy - panelHeight * 0.5f
                    panel.draw(
                        it,
                        left,
                        bottom,
                        panelWidth / 4f,
                        panelHeight / 4f,
                        scaleX = 4f,
                        scaleY = 4f,
                        color = panelColor,
                    )

                    val rectX = left + 140f
                    val rectY = cy + 90f
                    shapes.filledRectangle(
                        rectX - 45f,
                        rectY - 45f,
                        90f,
                        90f,
                        rotation = (time * 40f).degrees,
                        color = red,
                        color2 = orange,
                        color3 = yellow,
                        color4 = pink,
                    )
                    shapes.rectangle(
                        rectX - 65f,
                        rectY - 65f,
                        130f,
                        130f,
                        rotation = (-time * 25f).degrees,
                        thickness = 6f,
                        color = ink,
                    )

                    val triX = left + 140f
                    val triY = cy - 70f + sin(time * 2f) * 8f
                    shapes.filledTriangle(
                        triX - 50f,
                        triY - 40f,
                        triX + 50f,
                        triY - 40f,
                        triX,
                        triY + 45f,
                        color = red,
                        color2 = green,
                        color3 = blue,
                    )
                    shapes.triangle(
                        triX - 62f,
                        triY - 48f,
                        triX + 62f,
                        triY - 48f,
                        triX,
                        triY + 60f,
                        thickness = 4f,
                        color = ink,
                    )

                    val starX = cx
                    val starY = cy + 20f
                    shapes.circle(starX, starY, 125f, thickness = 2f, color = purple)
                    updateStar(starX, starY, 85f + sin(time * 4f) * 8f, 38f, time * 0.8f)
                    for (i in 0 until 10) {
                        val next = (i + 1) % 10
                        shapes.filledTriangle(
                            starX,
                            starY,
                            starPoints[i * 2],
                            starPoints[i * 2 + 1],
                            starPoints[next * 2],
                            starPoints[next * 2 + 1],
                            color = yellow,
                            color2 = orange,
                        )
                    }
                    shapes.path(starPoints, thickness = 5f, open = false, color = ink)
                    for (i in palette.indices) {
                        val angle = -time * 1.2f + i * PI2_F / palette.size
                        shapes.filledCircle(
                            starX + cos(angle) * 125f,
                            starY + sin(angle) * 125f,
                            12f + sin(time * 3f + i) * 3f,
                            color = palette[i],
                        )
                    }

                    val polyX = left + panelWidth - 140f
                    val polyY = cy + 90f
                    val pulse = sin(time * 2.5f)
                    shapes.polygon(
                        polyX,
                        polyY,
                        6,
                        80f,
                        rotation = (-time).radians,
                        thickness = 5f,
                        color = blue,
                    )
                    shapes.ellipse(
                        polyX,
                        polyY,
                        55f + pulse * 15f,
                        40f - pulse * 12f,
                        rotation = (time * 30f).degrees,
                        thickness = 4f,
                        color = green,
                    )
                    shapes.filledPolygon(
                        polyX,
                        polyY,
                        5,
                        24f,
                        rotation = (time * 2f).radians,
                        innerColor = Color.WHITE,
                        outerColor = pink,
                    )

                    val fanX = left + panelWidth - 140f
                    val fanY = cy - 70f
                    for (i in 0 until 8) {
                        val angle = time * 0.8f + i * PI2_F / 8f
                        shapes.line(
                            fanX,
                            fanY,
                            fanX + cos(angle) * 60f,
                            fanY + sin(angle) * 60f,
                            color = ink,
                            color2 = palette[i % palette.size],
                            thickness = 1f + i,
                        )
                    }
                    shapes.filledCircle(fanX, fanY, 8f, color = ink)

                    updateWave(left + 40f, bottom + 50f, panelWidth - 80f, time)
                    shapes.path(wavePoints, thickness = 6f, color = blue)
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            panelTexture.release()
        }
    }

    private companion object {
        const val WAVE_POINTS = 48
    }
}
