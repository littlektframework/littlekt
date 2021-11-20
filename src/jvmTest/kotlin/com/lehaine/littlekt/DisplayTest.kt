package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.fragment.TexturedFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.TexturedQuadShader
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.io.FloatBuffer
import com.lehaine.littlekt.io.ShortBuffer
import com.lehaine.littlekt.io.get

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val gl: GL get() = application.graphics.gl
    val batch = SpriteBatch(application)
    val input get() = application.input

    val texture by application.fileHandler.get<Texture>("person.png")
    val shader = ShaderProgram(gl, TexturedQuadShader(), TexturedFragmentShader())
    val mesh = Mesh(gl, true, 15, 3, VertexAttribute.POSITION, VertexAttribute.COLOR_PACKED)
    val vertices = floatArrayOf(
       -0.5f, -0.5f, 0f,
        0.5f, -0.5f, 0f,
        0f, 1f, 0f
    )
    val indices = shortArrayOf(0, 1, 2)

    val vbo: BufferReference
    val ibo: BufferReference


    init {
        vbo = gl.createBuffer()
        bindVbo()
        ibo = gl.createBuffer()
        bindIbo()
    }

    private fun bindVbo() {
        gl.bindBuffer(GL.ARRAY_BUFFER, vbo)
        val vboBuffer = FloatBuffer.allocate(vertices.size)
        vboBuffer.put(vertices)
        vboBuffer.flip()
        gl.bufferData(GL.ARRAY_BUFFER, DataSource.FloatBufferDataSource(vboBuffer), GL.STATIC_DRAW)
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
    }

    private fun bindIbo() {
        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, ibo)
        val buffer = ShortBuffer.allocate(indices.size)
        gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, DataSource.ShortBufferDataSource(buffer), GL.STATIC_DRAW)
    }

    override fun create() {
        println("create")
        mesh.setVertices(vertices)
        mesh.setIndices(indices)
        input.inputProcessor = this
    }

    override fun render(dt: Float) {
        gl.clearColor(0f, 0f, 0f, 0f)
        mesh.render(shader)
//        batch.begin()
//        batch.draw(texture, 5f, 5f)
//        batch.end()
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
}

fun main(args: Array<String>) {
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("Display Test", 960, 540, true) },
        gameBuilder = { DisplayTest(it) })
        .start()
}