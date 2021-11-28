package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class FrameBufferTest(application: Application) : LittleKt(application) {

    val batch = SpriteBatch(application)
    val fbo = FrameBuffer(240, 135).also { it.prepare(application) }
    var loading = true

    lateinit var texture: Texture

    var projection = Mat4().setOrthographic(
        left = 0f,
        right = graphics.width.toFloat(),
        bottom = 0f,
        top = graphics.height.toFloat(),
        near = -1f,
        far = 1f
    )
    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        fileHandler.launch {
            texture = loadTexture("person.png")
            loading = false
        }
    }

    override fun render(dt: Duration) {
        if (loading) {
            return
        }
        xVel = 0f
        yVel = 0f

        if (input.isKeyPressed(Key.W)) {
            yVel += 1f
        }
        if (input.isKeyPressed(Key.S)) {
            yVel -= 1f
        }
        if (input.isKeyPressed(Key.A)) {
            xVel -= 1f
        }
        if (input.isKeyPressed(Key.D)) {
            xVel += 1f
        }

        fbo.begin()
        projection.setOrthographic(
            left = 0f,
            right = fbo.width.toFloat(),
            bottom = 0f,
            top = fbo.height.toFloat(),
            near = -1f,
            far = 1f
        )
        gl.clearColor(Color.CLEAR)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        batch.use(projection) {
            it.color = Color.WHITE
            it.draw(texture, x, y)
            it.draw(texture, x, y+5)
        }
        fbo.end()

        batch.use(projection) {
            it.color = Color.WHITE.withAlpha(0.8f)
            it.draw(fbo.colorBufferTexture, 0f, 0f, flipY = true)

            it.color = Color.WHITE
            it.draw(texture, 100f, 50f)
        }
        x += xVel
        y += yVel

        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { engineStats }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }

    }

    override fun resize(width: Int, height: Int) {
        logger.debug { "Resize to $width,$height" }
    }

    override fun dispose() {
        texture.dispose()
        batch.dispose()
        fbo.dispose()
    }
}
