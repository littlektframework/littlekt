package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.createIntBuffer
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ScreenViewport
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 1/26/2024
 */
class FBOMultiTargetTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ScreenViewport(context.graphics.width, context.graphics.height)
        val camera = viewport.camera

        val fboCamera = OrthographicCamera(240, 135).apply {
            position.set(240 / 2f, 135 / 2f, 0f)
        }
        val fbo = FrameBuffer(
            240,
            135,
            listOf(
                FrameBuffer.ColorAttachment(minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST),
                FrameBuffer.ColorAttachment(minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST)
            )
        ).also {
            it.prepare(context)
        }
        val slice = fbo.textures[0].slice()
        val slice2 = fbo.textures[1].slice()
        val buffersToDraw = createIntBuffer(2).apply {
            put(GL.COLOR_ATTACHMENT0)
            put(GL.COLOR_ATTACHMENT1)
            flip()
        }

        var x = 0f
        var y = 0f
        var rotation = Angle.ZERO
        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            val speed = 0.2f * dt.milliseconds
            if (input.isKeyPressed(Key.W)) {
                y -= speed
            }
            if (input.isKeyPressed(Key.S)) {
                y += speed
            }
            if (input.isKeyPressed(Key.D)) {
                x += speed
            }
            if (input.isKeyPressed(Key.A)) {
                x -= speed
            }
            rotation += 0.01.radians

            if (abs(fboCamera.position.x - x) >= 25) {
                fboCamera.position.x = x
            }
            if (abs(fboCamera.position.y - y) >= 25) {
                fboCamera.position.y = y
            }
            fboCamera.update()
            fbo.use {
                gl.drawBuffers(2, buffersToDraw)
                gl.clearColor(0.5f, 0f, 0f, 1f)
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
                batch.use(fboCamera.viewProjection) {
                    it.draw(Textures.white, x, y, scaleX = 10f, scaleY = 10f, rotation = rotation)
                    it.draw(Textures.white, x + 20f, y + 20f, scaleX = 5f, scaleY = 5f, rotation = rotation)
                }
            }

            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            batch.use(camera.viewProjection) {
                var sx = graphics.width / 240f
                var sy = graphics.height / 135f
                sx = floor(sx)
                sy = floor(sy)

                val scale = max(1f, min(sx, sy))
                it.draw(slice, 0f, 0f, scaleX = scale, scaleY = scale, flipY = true)
                it.draw(slice2, 0f, 0f, scaleX = scale * 0.5f, scaleY = scale * 0.5f, flipY = true)
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