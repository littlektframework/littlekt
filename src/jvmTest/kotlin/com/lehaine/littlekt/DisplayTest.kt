package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.fragment.ColorFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.TexturedQuadShader
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.io.get
import com.lehaine.littlekt.math.ortho

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val gl: GL get() = application.graphics.gl
    val batch = SpriteBatch(application)
    val input get() = application.input

    val texture by application.fileHandler.get<Texture>("person.png")
    val shader = ShaderProgram(gl, TexturedQuadShader(), ColorFragmentShader())
    val mesh = Mesh(gl, true, 4, 6, VertexAttribute.POSITION, VertexAttribute.COLOR_UNPACKED)

    val vertices = floatArrayOf(
        -50f, -50f, 0f, 1f, 1f, 1f, 1f,
        50f, -50f, 0f, 1f, 1f, 1f, 1f,
        50f, 50f, 0f, 1f, 1f, 1f, 1f,
        -50f, 50f, 0f, 1f, 1f, 1f, 1f,
    )
    val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

    override fun create() {
        println("create")
        mesh.setIndices(indices)
        mesh.setVertices(vertices)
        input.inputProcessor = this
    }

    var projection = ortho(
        l = 0f,
        r = application.graphics.width.toFloat(),
        b = 0f,
        t = application.graphics.height.toFloat(),
        n = -1f,
        f = 1f
    )

    override fun render(dt: Float) {
        gl.clearColor(0f, 0f, 0f, 0f)
//        batch.begin()
//        batch.draw(texture, 0f, 0f)
//        batch.end()

        shader.bind()
        shader.vertexShader.uProjTrans.apply(shader, projection)
        mesh.render(shader)
    }

    override fun resize(width: Int, height: Int) {
        projection = ortho(
            l = 0f,
            r = application.graphics.width.toFloat(),
            b = 0f,
            t = application.graphics.height.toFloat(),
            n = -1f,
            f = 1f
        )
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
}

fun main(args: Array<String>) {
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("Display Test", 960, 540, true) },
        gameBuilder = { DisplayTest(it) })
        .start()
}