package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.IndexType
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 11/18/2021
 */

inline fun mesh(gl: GL, attributes: List<VertexAttribute>, block: MeshProps.() -> Unit): Mesh {
    val props = MeshProps().apply(block)
    return Mesh(gl, props.isStatic, props.maxVertices, props.maxIndices, attributes)
}

fun colorMesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED), generate)
}

fun textureMesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(
        gl,
        listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED, VertexAttribute.TEX_COORDS(0)),
        generate
    )
}

fun Application.mesh(attributes: List<VertexAttribute>, block: MeshProps.() -> Unit): Mesh {
    return mesh(gl, attributes, block)
}

fun Application.colorMesh(generate: MeshProps.() -> Unit): Mesh {
    return colorMesh(gl, generate)
}

fun Application.textureMesh(generate: MeshProps.() -> Unit): Mesh {
    return textureMesh(gl, generate)
}

class MeshProps {
    var isStatic: Boolean = true
    var maxVertices = 1000
    var maxIndices = maxVertices * 6
}

class MeshBatcher(size: Int, val attributes: VertexAttributes) {
    val vertices = FloatArray(size)

    private var offset = 0
    fun setVertex(props: VertexProps) {
        attributes.forEach { vertexAttribute ->
            when (vertexAttribute.usage) {
                VertexAttrUsage.POSITION_2D -> {
                    vertices[offset++] = props.x
                    vertices[offset++] = props.y
                }
                VertexAttrUsage.COLOR_UNPACKED -> {
                    vertices[offset++] = props.color.r
                    vertices[offset++] = props.color.g
                    vertices[offset++] = props.color.b
                    vertices[offset++] = props.color.a
                }
                VertexAttrUsage.COLOR_PACKED -> {
                    vertices[offset++] = props.colorPacked
                }
                VertexAttrUsage.TEX_COORDS -> {
                    vertices[offset++] = props.u
                    vertices[offset++] = props.v
                }
            }
        }
    }

    fun reset() {
        offset = 0
    }

}

class VertexProps {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var colorPacked: Float = 0f
    var color: Color = Color.CLEAR
    var u: Float = 0f
    var v: Float = 0f
}

class Mesh(
    val gl: GL,
    isStatic: Boolean,
    val maxVertices: Int,
    val maxIndices: Int,
    attributes: List<VertexAttribute>
) :
    Disposable {

    private val vertexAttributes = VertexAttributes(attributes)
    private val vertices = VertexBufferObject(gl, isStatic, maxVertices, vertexAttributes)
    private val indices = IndexBufferObject(gl, maxIndices, isStatic)
    private var updateVertices = false
    private val tempVertexProps = VertexProps()

    val batcher = MeshBatcher(vertices.maxNumVertices, vertexAttributes)

    val verticesBuffer get() = vertices.buffer
    val indicesBuffer get() = indices.buffer

    val numIndices get() = indices.numIndices
    val numVertices get() = vertices.numVertices

    fun setVertex(action: VertexProps.() -> Unit) {
        tempVertexProps.action()
        batcher.setVertex(tempVertexProps)
        updateVertices = true
    }

    fun setIndicesAsTriangle() {
        val indices = ShortArray(maxIndices)
        var i = 0
        var j = 0
        while (i < maxIndices) {
            indices[i] = j.toShort()
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j.toShort()
            i += 6
            j += 4
        }
        setIndices(indices)
    }

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
        drawMode: DrawMode = DrawMode.TRIANGLES,
        offset: Int = 0,
        count: Int = if (numIndices > 0) numIndices else numVertices,
    ) {
        if (count == 0) {
            return
        }
        if (updateVertices) {
            setVertices(batcher.vertices)
            batcher.reset()
            updateVertices = false
        }
        bind(shader)
        if (numIndices > 0) {
            if (count + offset > indices.maxNumIndices) {
                throw RuntimeException("Mesh attempting to access memory outside of the index buffer (count: $count, offset: $offset, max: $numIndices)")
            }
            gl.drawElements(drawMode, count, IndexType.UNSIGNED_SHORT, offset * 2)
        } else {
            gl.drawArrays(drawMode, offset, count)
        }
        unbind(shader)
    }

    private fun resetProps() {
        tempVertexProps.apply {
            x = 0f
            y = 0f
            z = 0f
            colorPacked = 0f
            color = Color.CLEAR
            u = 0f
            v = 0f

        }
    }

    override fun dispose() {
        vertices.dispose()
        indices.dispose()
    }
}