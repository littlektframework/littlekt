package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.GL
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.io.FloatBuffer
import com.lehaine.littlekt.io.ShortBuffer

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
class VertexBufferObject(val gl: GL, val isStatic: Boolean, numVertices: Int, val attributes: VertexAttributes) :
    Disposable {
    private val buffer: FloatBuffer = FloatBuffer.allocate(attributes.vertexSize / 4 * numVertices)
    private val bufferReference: BufferReference = gl.createBuffer()
    private val usage = if (isStatic) GL.STATIC_DRAW else GL.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numVertices get() = buffer.limit * 4 / attributes.vertexSize
    val maxNumVertices get() = buffer.capacity / attributes.vertexSize

    private var isDirty = false

    init {
        buffer.flip()
    }

    fun setVertices(vertices: FloatArray, srcOffset: Int, count: Int) {
        isDirty = true
        buffer.clear()
        buffer.put(vertices, srcOffset, count)
        buffer.position = 0
        buffer.limit = count
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, vertices: FloatArray, srcOffset: Int, count: Int) {
        isDirty = true
        val pos = buffer.position
        buffer.position = destOffset
        buffer.put(vertices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    fun bind(shader: ShaderProgram, locations: IntArray? = null) {
        gl.bindBuffer(GL.ARRAY_BUFFER, bufferReference)
        if (isDirty) {
            gl.bufferData(GL.ARRAY_BUFFER, DataSource.FloatBufferDataSource(buffer), usage)
            isDirty = false
        }
        attributes.forEachIndexed { index, attribute ->
            val location = locations?.get(index) ?: shader.getAttrib(attribute.alias)
            if (location < 0) {
                return@forEachIndexed
            }
            gl.enableVertexAttribArray(location)
            gl.vertexAttribPointer(
                location,
                attribute.numComponents,
                attribute.type,
                attribute.normalized,
                attributes.vertexSize,
                attribute.offset
            )
        }
        bound = true
    }

    fun unbind(shader: ShaderProgram, locations: IntArray?) {
        attributes.forEachIndexed { index, attribute ->
            gl.disableVertexAttribArray(locations?.get(index) ?: shader.getAttrib(attribute.alias))
        }
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferData(GL.ARRAY_BUFFER, DataSource.FloatBufferDataSource(buffer), usage)
            isDirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        gl.deleteBuffer(bufferReference)
    }

}

class IndexBufferObject(val gl: GL, maxIndices: Int, val isStatic: Boolean = true) : Disposable {
    private val bufferReference: BufferReference = gl.createBuffer()
    private val buffer = ShortBuffer.allocate(maxIndices * 2)
    private val usage = if (isStatic) GL.STATIC_DRAW else GL.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numIndices get() = buffer.limit
    val maxNumIndices get() = buffer.capacity
    private var isDirty = false

    init {
        buffer.flip()
    }

    fun setIndices(indices: ShortArray, srcOffset: Int, count: Int) {
        buffer.clear()
        buffer.put(indices, srcOffset, count)
        buffer.flip()
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, indices: ShortArray, srcOffset: Int, count: Int) {
        val pos = buffer.position
        buffer.position = destOffset
        buffer.put(indices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    fun bind() {
        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, bufferReference)
        bound = true
    }

    fun unbind() {
        gl.bindDefaultBuffer(GL.ELEMENT_ARRAY_BUFFER)
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, DataSource.ShortBufferDataSource(buffer), usage)
            isDirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ELEMENT_ARRAY_BUFFER)
        gl.deleteBuffer(bufferReference)
    }
}