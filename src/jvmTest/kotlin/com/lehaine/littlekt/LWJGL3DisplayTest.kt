package com.lehaine.littlekt

import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

/**
 * @author Colton Daily
 * @date 11/6/2021
 */

class LWJGL3DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val input get() = application.input
    val vertices = floatArrayOf(
        0.5f, -0.5f, 0f,
        0.5f, -0.5f, 0f,
        0f, 0.5f, 0f
    )
    val indices = intArrayOf(0, 1, 2)

    val vboLwjgl: Int
    val iboLwjgl: Int
    val vao: Int


    init {
        vao = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vao)
        vboLwjgl = GL15.glGenBuffers()
        bindLwjglVbo()
        iboLwjgl = GL15.glGenBuffers()
        bindLwjglIbo()
        GL30.glBindVertexArray(0)
    }

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
        val buffer = BufferUtils.createIntBuffer(indices.size)
        buffer.put(indices)
        buffer.flip()
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
    }

    override fun create() {
        println("create")
        input.inputProcessor = this

    }

    override fun render(dt: Float) {
        GL11.glClearColor(0f, 0f, 0f, 0f)

        GL30.glBindVertexArray(vao)
        GL20.glEnableVertexAttribArray(0)
        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.size, GL15.GL_UNSIGNED_INT, 0)
        GL20.glDisableVertexAttribArray(0)
        GL30.glBindVertexArray(0)
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