package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.fragment.TexturedFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.TexturedQuadShader
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.io.get
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class LWJGL3DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val gl: GL get() = application.graphics.gl
    val batch = SpriteBatch(application)
    val input get() = application.input

    val texture by application.fileHandler.get<Texture>("person.png")
    val shader = ShaderProgram(gl, TexturedQuadShader(), TexturedFragmentShader())
    val mesh = Mesh(gl, true, 15, 3, VertexAttribute.POSITION)
    val vertices = floatArrayOf(
        0.5f, -0.5f, 0f,
        0.5f, -0.5f, 0f,
        0f, 0.5f, 0f
    )
    val indices = shortArrayOf(0, 1, 2)

    //    val vbo: BufferReference
//    val ibo: BufferReference
    val vboLwjgl: Int
    val iboLwjgl: Int
    val vao: Int

    init {
//        vbo = gl.createBuffer()
//        bindVbo()
//        ibo = gl.createBuffer()
//        bindIbo()
        vao = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vao)
        vboLwjgl = GL15.glGenBuffers()
        bindLwjglVbo()
        iboLwjgl = GL15.glGenBuffers()
        bindLwjglIbo()
    }

//    private fun bindVbo() {
//        gl.bindBuffer(GL.ARRAY_BUFFER, vbo)
//        val vboBuffer = FloatBuffer.allocate(vertices.size)
//        vboBuffer.put(vertices)
//        vboBuffer.flip()
//        gl.bufferData(GL.ARRAY_BUFFER, DataSource.FloatBufferDataSource(vboBuffer), GL.STATIC_DRAW)
//        gl.vertexAttribPointer(0, 3, GL.FLOAT, false, 0, 0)
//        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
//    }
//
//    private fun bindIbo() {
//        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, ibo)
//        val buffer = ShortBuffer.allocate(indices.size)
//        gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, DataSource.ShortBufferDataSource(buffer), GL.STATIC_DRAW)
//    }

    private fun bindLwjglVbo() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboLwjgl)
        val vboBuffer = BufferUtils.createFloatBuffer(vertices.size)
        vboBuffer.put(vertices)
        vboBuffer.flip()
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuffer, GL15.GL_STATIC_DRAW)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    }

    private fun bindLwjglIbo() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboLwjgl)
        val buffer = BufferUtils.createShortBuffer(indices.size)
        buffer.put(indices)
        buffer.flip()
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
    }

    override fun create() {
        println("create")
//        mesh.setVertices(vertices)
//        mesh.setIndices(indices)
        input.inputProcessor = this
    }

    override fun render(dt: Float) {
        gl.clearColor(0f, 0f, 0f, 0f)
        GL30.glBindVertexArray(vao)
//        gl.enableVertexAttribArray(0)
//        gl.drawElements(GL.TRIANGLES, indices.size, GL.UNSIGNED_SHORT, 0)
//        gl.disableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(0)
        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.size, GL15.GL_UNSIGNED_SHORT, 0)
        GL20.glDisableVertexAttribArray(0)
        GL30.glBindVertexArray(0)
        //      mesh.render()
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
        configBuilder = { ApplicationConfiguration("LWJGL3 Display Test", 960, 540, true) },
        gameBuilder = { LWJGL3DisplayTest(it) })
        .start()
}