package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.fragment.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.SimpleColorVertexShader
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val batch = SpriteBatch(application)

    var loading = true

    lateinit var texture: Texture
    val shader = createShader(SimpleColorVertexShader(), SimpleColorFragmentShader())
    val colorBits = Color.WHITE.toFloatBits()
    val mesh = colorMesh {
        maxVertices = 4
    }.apply {
        setVertex {
            x = 50f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 66f
            colorPacked = colorBits
        }

        setVertex {
            x = 50f
            y = 66f
            colorPacked = colorBits
        }

        setIndicesAsTriangle()
    }

    val camera = OrthographicCamera().apply {
        left = 0f
        right = graphics.width.toFloat()
        bottom = 0f
        top = graphics.height.toFloat()
    }
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
        input.inputProcessor = this
    }

    override fun render(dt: Float) {
        if (loading) {
            return
        }
        xVel = 0f
        yVel = 0f

        if (input.isKeyPressed(Key.W)) {
            yVel += 10f
        }
        if (input.isKeyPressed(Key.S)) {
            yVel -= 10f
        }
        if (input.isKeyPressed(Key.A)) {
            xVel -= 10f
        }
        if (input.isKeyPressed(Key.D)) {
            xVel += 10f
        }

        gl.clearColor(Color.CLEAR)
        camera.update()
        batch.use(camera.viewProjection) {
            it.draw(texture, x, y, scaleX = 10f, scaleY = 10f)
            it.draw(texture, 50f, 50f, scaleX = 5f, scaleY = 5f)
            it.draw(texture, 750f, 175f, scaleX = 2f, scaleY = 2f)
            it.draw(texture, 375f, 400f, scaleX = 3f, scaleY = 7f)
            it.draw(texture, 525f, 100f, scaleX = 7f, scaleY = 3f)
            it.draw(Texture.DEFAULT, 100f, 100f, scaleX = 5f, scaleY = 5f)
        }

        shader.bind()
        shader.uProjTrans?.apply(shader, camera.viewProjection)
        mesh.render(shader)

        x += xVel
        y += yVel


        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { engineStats }
        }

    }

    override fun resize(width: Int, height: Int) {
        logger.debug { "Resize to $width,$height" }
    }

    override fun keyDown(key: Key): Boolean {
        logger.debug { "Key down: $key" }
        if (key == Key.ESCAPE) {
            application.close()
        }
        return false
    }

    override fun keyUp(key: Key): Boolean {
        logger.debug { "Key up: $key" }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        logger.debug { "Key typed: $character" }
        return false
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        logger.debug { "Mouse button $pointer pressed $screenX,$screenY" }
        return false
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        logger.debug { "Mouse button $pointer released $screenX,$screenY" }
        return false
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        logger.debug { "Mouse button dragged to $screenX,$screenY" }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        logger.debug { "Scrolled $amountX,$amountY" }
        return false
    }

    override fun dispose() {
        mesh.dispose()
        texture.dispose()
        shader.dispose()
        batch.dispose()
    }
}
