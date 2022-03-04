package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.DataSource
import com.lehaine.littlekt.file.FloatBuffer
import com.lehaine.littlekt.file.createFloatBuffer
import com.lehaine.littlekt.file.createShortBuffer
import com.lehaine.littlekt.graphics.gl.BufferTarget
import com.lehaine.littlekt.graphics.gl.GlBuffer
import com.lehaine.littlekt.graphics.gl.GlVertexArray
import com.lehaine.littlekt.graphics.gl.Usage
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
class VertexBufferObject(val gl: GL, val isStatic: Boolean, numVertices: Int, val attributes: VertexAttributes) :
    Disposable {
    val buffer: FloatBuffer = createFloatBuffer(attributes.vertexSize / 4 * numVertices)
        get() {
            isDirty = true
            return field
        }
    private val glBuffer: GlBuffer = gl.createBuffer()
    private val vaoGl: GlVertexArray? = if (gl.isG30) gl.createVertexArray() else null
    private val usage = if (isStatic) Usage.STATIC_DRAW else Usage.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numVertices get() = buffer.limit * 4 / attributes.vertexSize
    val maxNumVertices get() = buffer.capacity * 4 / attributes.vertexSize

    private var isDirty = false

    init {
        allocBuffer()
        buffer.flip()
    }

    private fun allocBuffer() {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        gl.bufferData(BufferTarget.ARRAY, DataSource.FloatBufferDataSource(buffer), usage)
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        vaoGl?.let {
            gl.bindDefaultVertexArray()
        }
    }

    fun setVertices(vertices: FloatArray, srcOffset: Int, count: Int) {
        isDirty = true
        buffer.clear()
        buffer.position = 0
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

    fun bind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        if (isDirty) {
            gl.bufferSubData(BufferTarget.ARRAY, 0, DataSource.FloatBufferDataSource(buffer))
            isDirty = false
        }
        if (shader != null) {
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
        }
        bound = true
    }

    fun unbind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        if (shader != null) {
            attributes.forEachIndexed { index, attribute ->
                gl.disableVertexAttribArray(locations?.get(index) ?: shader.getAttrib(attribute.alias))
            }
        }
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        vaoGl?.let {
            gl.bindDefaultVertexArray()
        }
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferSubData(BufferTarget.ARRAY, 0, DataSource.FloatBufferDataSource(buffer))
            isDirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        gl.deleteBuffer(glBuffer)
    }

}

class IndexBufferObject(val gl: GL, maxIndices: Int, val isStatic: Boolean = true) : Disposable {
    val buffer = createShortBuffer(maxIndices * 2)
        get() {
            isDirty = true
            return field
        }
    private val glBuffer: GlBuffer = gl.createBuffer()
    private val usage = if (isStatic) Usage.STATIC_DRAW else Usage.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numIndices get() = buffer.limit
    val maxNumIndices get() = buffer.capacity
    private var isDirty = false

    init {
        allocBuffer()
        buffer.flip()
    }

    private fun allocBuffer() {
        gl.bindBuffer(BufferTarget.ELEMENT_ARRAY, glBuffer)
        gl.bufferData(BufferTarget.ELEMENT_ARRAY, DataSource.ShortBufferDataSource(buffer), usage)
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
    }

    fun setIndices(indices: ShortArray, srcOffset: Int, count: Int) {
        isDirty = true
        buffer.clear()
        buffer.put(indices, srcOffset, count)
        buffer.flip()
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, indices: ShortArray, srcOffset: Int, count: Int) {
        isDirty = true
        val pos = buffer.position
        buffer.position = destOffset
        buffer.put(indices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    fun bind() {
        gl.bindBuffer(BufferTarget.ELEMENT_ARRAY, glBuffer)
        if (isDirty) {
            gl.bufferSubData(BufferTarget.ELEMENT_ARRAY, 0, DataSource.ShortBufferDataSource(buffer))
            isDirty = false
        }
        bound = true
    }

    fun unbind() {
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferSubData(BufferTarget.ELEMENT_ARRAY, 0, DataSource.ShortBufferDataSource(buffer))
            isDirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
        gl.deleteBuffer(glBuffer)
    }
}