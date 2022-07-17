package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
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
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val animPlayer = AnimationPlayer<TextureSlice>()
        val heroSleep = atlas.getAnimation("heroSleeping")

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
                shapeRenderer.triangle(50f, 250f, 50f, Color.GREEN)
                shapeRenderer.rect(50f, 100f, 50f, 50f, Color.RED)
                shapeRenderer.circle(155f, 75f, 50f)
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