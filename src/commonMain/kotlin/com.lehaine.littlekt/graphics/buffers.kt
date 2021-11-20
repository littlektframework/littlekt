package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.GL
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
class VertexBufferObject(val gl: GL, val isStatic: Boolean, numVertices: Int, val attributes: VertexAttributes) :
    Disposable {
    private val bufferReference: Buffer = gl.createBuffer()
    private val buffer = FloatArray(attributes.vertexSize * numVertices)
    private val usage = if (isStatic) GL.STATIC_DRAW else GL.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numVertices = buffer.size * 4 / attributes.vertexSize

    fun setVertices(vertices: FloatArray, srcOffset: Int, count: Int) {
        vertices.copyInto(buffer, 0, srcOffset, count)
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, vertices: FloatArray, srcOffset: Int, count: Int) {
        vertices.copyInto(buffer, destOffset, srcOffset, count)
        onBufferChanged()
    }

    fun bind(shader: ShaderProgram, locations: IntArray? = null) {
        gl.bindBuffer(GL.ARRAY_BUFFER, bufferReference)
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
            gl.bufferData(GL.ARRAY_BUFFER, DataSource.FloatDataSource(buffer), usage)
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        gl.deleteBuffer(bufferReference)
    }

}

class IndexBufferObject(val gl: GL, maxIndices: Int, val isStatic: Boolean = true) : Disposable {
    private val bufferReference: Buffer = gl.createBuffer()
    private val buffer = ShortArray(maxIndices * 2)
    private val usage = if (isStatic) GL.STATIC_DRAW else GL.DYNAMIC_DRAW
    private var bound = false

    val isBound get() = bound
    val numIndices = buffer.size

    fun setIndices(indices: ShortArray, srcOffset: Int, count: Int) {
        indices.copyInto(buffer, 0, srcOffset, count)
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, indices: ShortArray, srcOffset: Int, count: Int) {
        indices.copyInto(buffer, destOffset, srcOffset, count)
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
            gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, DataSource.ShortDataSource(buffer), usage)
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ELEMENT_ARRAY_BUFFER)
        gl.deleteBuffer(bufferReference)
    }
}