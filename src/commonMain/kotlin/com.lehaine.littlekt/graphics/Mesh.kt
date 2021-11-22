package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class Mesh(val gl: GL, isStatic: Boolean, maxVertices: Int, maxIndices: Int, vararg attributes: VertexAttribute) :
    Disposable {

    private val vertices = VertexBufferObject(gl, isStatic, maxVertices, VertexAttributes(*attributes))
    private val indices = IndexBufferObject(gl, maxIndices, isStatic)

    val verticesBuffer get() = vertices.buffer
    val indicesBuffer get() = indices.buffer

    val numIndices get() = indices.numIndices
    val numVertices get() = vertices.numVertices

    fun setVertices(vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size): Mesh {
        this.vertices.setVertices(vertices, srcOffset, count)
        return this
    }

    fun updateVertices(destOffset: Int, source: FloatArray, srcOffset: Int = 0, count: Int = source.size): Mesh {
        this.vertices.updateVertices(destOffset, source, srcOffset, count)
        return this
    }

    fun setIndices(indices: ShortArray, srcOffset: Int = 0, count: Int = indices.size): Mesh {
        this.indices.setIndices(indices, srcOffset, count)
        return this
    }

    fun bind(shader: ShaderProgram? = null, locations: IntArray? = null) {
        vertices.bind(shader, locations)
        if (numIndices > 0) {
            indices.bind()
        }
    }

    fun unbind(shader: ShaderProgram? = null, locations: IntArray? = null) {
        vertices.unbind(shader, locations)
        if (numIndices > 0) {
            indices.unbind()
        }
    }

    fun render(
        shader: ShaderProgram? = null,
        primitiveType: Int = GL.TRIANGLES,
        offset: Int = 0,
        count: Int = if (numIndices > 0) numIndices else numVertices,
    ) {
        if (count == 0) {
            return
        }
        bind(shader)
        if (numIndices > 0) {
            if (count + offset > indices.maxNumIndices) {
                throw RuntimeException("Mesh attempting to access memory outside of the index buffer (count: $count, offset: $offset, max: $numIndices)")
            }
            gl.drawElements(primitiveType, count, GL.UNSIGNED_SHORT, offset * 2)
        } else {
            gl.drawArrays(primitiveType, offset, count)
        }
        unbind(shader)
    }

    override fun dispose() {
        vertices.dispose()
        indices.dispose()
    }
}