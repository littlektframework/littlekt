package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.g2d.*
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.combine
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRendererTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas().combine(Textures.white, "pixel", this)
        val batch = SpriteBatch(this)
        val shapeRenderer = ShapeRenderer(batch, atlas["pixel"].slice)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val animPlayer = AnimationPlayer<TextureSlice>()
        val heroSleep = atlas.getAnimation("heroSleeping")

        val path = listOf(
            Vec2f(0f, 0f),
            Vec2f(50f, 25f),
            Vec2f(135f, 232f),
            Vec2f(312f, 400f),
            Vec2f(650f, 425f),
            Vec2f(725f, 50f),
            Vec2f(250f, 10f)
        )

        animPlayer.playLooped(heroSleep)

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            animPlayer.update(dt)
            camera.update()
            batch.use(camera.viewProjection) { batch ->
                animPlayer.currentKeyFrame?.let {
                    batch.draw(it, 250f, 50f, scaleX = 5f, scaleY = 5f)
                }
                shapeRenderer.run {
                    colorBits = Color.YELLOW.toFloatBits()
                    ellipse(400f, 365f, 25f, 50f)
                    colorBits = Color.GREEN.toFloatBits()
                    circle(500f, 365f, 150f, thickness = 10)
                    line(125f, 100f, 225f, 175f, Color.BLUE, Color.YELLOW, snap = true)
                    line(125f, 175f, 225f, 100f, Color.BLUE)
                    line(125f, 100f, 225f, 100f, Color.BLUE)
                    colorBits = Color.RED.toFloatBits()
                    rectangle(50f, 50f, 75f, 50f)
                    triangle(100f, 250f, 150f, 300f, 200f, 250f)
                    colorBits = Color.ORANGE.toFloatBits()
                    path(path, thickness = 4)
                    colorBits = Color.LIGHT_BLUE.toFloatBits()
                    rectangle(500f, 400f, 100f, 50f, 45.degrees)
                    triangle(75f, 450f, 150f, 350f, 200f, 500f, thickness = 4)
                    filledEllipse(
                        600f,
                        200f,
                        25f,
                        40f,
                        33.degrees,
                        innerColor = Color.DARK_GRAY.toFloatBits(),
                        outerColor = Color.WHITE.toFloatBits()
                    )
                    filledCircle(600f, 280f, 40f, color = Color.LIGHT_RED.toFloatBits())
                    filledRectangle(600f, 400f, 20f, 30f, 45.degrees)
                    filledTriangle(450f, 150f, 525f, 50f, 400f, 400f)
                }
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}