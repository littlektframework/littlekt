package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.FloatBuffer
import com.lehaine.littlekt.file.ShortBuffer
import com.lehaine.littlekt.file.createFloatBuffer
import com.lehaine.littlekt.file.createShortBuffer
import com.lehaine.littlekt.graphics.gl.BufferTarget
import com.lehaine.littlekt.graphics.gl.GlBuffer
import com.lehaine.littlekt.graphics.gl.GlVertexArray
import com.lehaine.littlekt.graphics.gl.Usage
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import kotlin.math.max
import kotlin.math.round


/**
 * @author Colton Daily
 * @date 11/19/2021
 */
class VertexBufferObject(
    val gl: GL,
    val isStatic: Boolean,
    val attributes: VertexAttributes,
    var buffer: FloatBuffer,
) : Disposable {
    private val glBuffer: GlBuffer = gl.createBuffer()
    private val vaoGl: GlVertexArray? = if (gl.isG30) gl.createVertexArray() else null
    private val usage = if (isStatic) Usage.STATIC_DRAW else Usage.DYNAMIC_DRAW
    private var bound = false

    var growFactor = 2f
    var grow = false

    val isBound get() = bound
    val numVertices get() = buffer.limit * 4 / attributes.vertexSize
    val maxNumVertices get() = buffer.capacity * 4 / attributes.vertexSize

    constructor(gl: GL, isStatic: Boolean, numVertices: Int, attributes: VertexAttributes) : this(
        gl,
        isStatic,
        attributes,
        createFloatBuffer(attributes.vertexSize / 4 * numVertices)
    )

    init {
        allocBuffer()
        buffer.flip()
    }

    private fun allocBuffer() {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        gl.bufferData(BufferTarget.ARRAY, buffer, usage)
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        vaoGl?.let {
            gl.bindDefaultVertexArray()
        }
    }

    fun setVertices(vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size) {
        buffer.clear()
        buffer.position = 0
        checkBufferSizes(count)
        buffer.put(vertices, srcOffset, count)
        buffer.position = 0
        buffer.limit = count
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size) {
        val pos = buffer.position
        buffer.position = destOffset
        checkBufferSizes(count)
        buffer.put(vertices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    private fun checkBufferSizes(reqSpace: Int) {
        if (!grow) return
        if (buffer.remaining < reqSpace) {
            increaseBufferSize(
                max(
                    round(buffer.capacity * growFactor).toInt(),
                    (numVertices + reqSpace) * attributes.vertexSize
                )
            )
        }
    }

    private fun increaseBufferSize(newSize: Int) {
        val newData = createFloatBuffer(newSize)
        buffer.flip()
        newData.put(buffer)
        buffer = newData
    }

    fun bind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        vaoGl?.let {
            gl.bindVertexArray(it)
        }
        gl.bindBuffer(BufferTarget.ARRAY, glBuffer)
        if (buffer.dirty) {
            gl.bufferSubData(BufferTarget.ARRAY, 0, buffer)
            buffer.dirty = false
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
            gl.bufferSubData(BufferTarget.ARRAY, 0, buffer)
            buffer.dirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(GL.ARRAY_BUFFER)
        gl.deleteBuffer(glBuffer)
    }

}

class IndexBufferObject(
    val gl: GL,
    val isStatic: Boolean = true,
    var buffer: ShortBuffer,
) : Disposable {

    private val glBuffer: GlBuffer = gl.createBuffer()
    private val usage = if (isStatic) Usage.STATIC_DRAW else Usage.DYNAMIC_DRAW
    private var bound = false

    var growFactor = 2f
    var grow = false

    val isBound get() = bound
    val numIndices get() = buffer.limit
    val maxNumIndices get() = buffer.capacity

    constructor(gl: GL, isStatic: Boolean = true, numIndices: Int) : this(
        gl,
        isStatic,
        createShortBuffer(numIndices * 2)
    )

    init {
        allocBuffer()
        buffer.flip()
    }

    private fun allocBuffer() {
        gl.bindBuffer(BufferTarget.ELEMENT_ARRAY, glBuffer)
        gl.bufferData(BufferTarget.ELEMENT_ARRAY, buffer, usage)
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
    }

    fun setIndices(indices: ShortArray, srcOffset: Int = 0, count: Int = indices.size) {
        buffer.clear()
        checkBufferSizes(count)
        buffer.put(indices, srcOffset, count)
        buffer.flip()
        onBufferChanged()
    }

    fun updateVertices(destOffset: Int, indices: ShortArray, srcOffset: Int = 0, count: Int = indices.size) {
        val pos = buffer.position
        buffer.position = destOffset
        checkBufferSizes(count)
        buffer.put(indices, srcOffset, count)
        buffer.position = pos
        onBufferChanged()
    }

    private fun checkBufferSizes(reqSpace: Int) {
        if (!grow) return
        if (buffer.remaining < reqSpace) {
            increaseBufferSize(
                max(
                    round(buffer.capacity * growFactor).toInt(),
                    numIndices + reqSpace
                )
            )
        }
    }

    private fun increaseBufferSize(newSize: Int) {
        val newData = createShortBuffer(newSize)
        buffer.flip()
        newData.put(buffer)
        buffer = newData
    }

    fun bind() {
        gl.bindBuffer(BufferTarget.ELEMENT_ARRAY, glBuffer)
        if (buffer.dirty) {
            gl.bufferSubData(BufferTarget.ELEMENT_ARRAY, 0, buffer)
            buffer.dirty = false
        }
        bound = true
    }

    fun unbind() {
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
        bound = false
    }

    private fun onBufferChanged() {
        if (bound) {
            gl.bufferSubData(BufferTarget.ELEMENT_ARRAY, 0, buffer)
            buffer.dirty = false
        }
    }

    override fun dispose() {
        gl.bindDefaultBuffer(BufferTarget.ELEMENT_ARRAY)
        gl.deleteBuffer(glBuffer)
    }
}