package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import com.lehaine.littlekt.util.viewport.ScreenViewport
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 3/15/2022
 */
class FBOTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ScreenViewport(context.graphics.width, context.graphics.height)
        val camera = viewport.camera

        val fboCamera = OrthographicCamera(240, 135).apply {
            position.set(240 / 2f, 135 / 2f, 0f)
        }
        val fbo = FrameBuffer(240, 135, minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST).also {
            it.prepare(context)
        }
        val slice = fbo.colorBufferTexture.slice()

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
            fbo.begin()
            gl.clearColor(0.5f, 0f, 0f, 1f)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            batch.use(fboCamera.viewProjection) {
                it.draw(Textures.white, x, y, scaleX = 10f, scaleY = 10f, rotation = rotation)
            }
            fbo.end()

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