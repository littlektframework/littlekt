package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.ortho

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val batch = SpriteBatch(application)

    var loading = true

    //   val texture by application.fileHandler.get<Texture>("person.png")
    lateinit var texture: Texture
    val shader = createShader()
    val mesh =
        Mesh(gl, true, 4, 6, VertexAttribute.POSITION, VertexAttribute.COLOR_PACKED)
    val packedColor = Color.WHITE.toFloatBits()

    val vertices = floatArrayOf(
        50f, 50f, packedColor,
        66f, 50f, packedColor,
        66f, 66f, packedColor,
        50f, 66f, packedColor
    )
    val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

    init {
        fileHandler.launch {
            texture = loadTexture("person.png")
            println("we got a texture loaded ${texture.width},${texture.height}")
            loading = false
        }
    }

    override fun create() {
        println("create")
        mesh.setIndices(indices)
        mesh.setVertices(vertices)
        input.inputProcessor = this
    }

    var projection = ortho(
        l = 0f,
        r = 480f * 2,
        b = 0f,
        t = 270f * 2,
        n = -1f,
        f = 1f
    )
    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f

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
        batch.use {
            it.draw(texture, x, y, scaleX = 10f, scaleY = 10f)
            it.draw(Texture.DEFAULT, 100f, 100f, scaleX = 5f, scaleY = 5f)
        }

        shader.bind()
        shader.vertexShader.uProjTrans.apply(shader, projection)
        mesh.render(shader)

        x += xVel
        y += yVel
    }

    override fun resize(width: Int, height: Int) {
        println("resize to $width,$height")
    }

    override fun keyDown(key: Key): Boolean {
        println("Key down: $key")
        if (key == Key.ESCAPE) {
            application.close()
        }
        return false
    }

    override fun keyUp(key: Key): Boolean {
        println("key up $key")
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        println("Key typed $character")
        return false
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button $pointer pressed $screenX,$screenY")
        return false
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button $pointer released $screenX,$screenY")
        return false
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button dragged to $screenX,$screenY")
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        println("Scrolled $amountX,$amountY")
        return false
    }

    override fun dispose() {
        mesh.dispose()
        texture.dispose()
        shader.dispose()
    }
}
