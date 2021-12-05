package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.IndexType
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import kotlin.math.floor

/**
 * @author Colton Daily
 * @date 11/18/2021
 */

inline fun mesh(gl: GL, attributes: List<VertexAttribute>, block: MeshProps.() -> Unit): Mesh {
    val props = MeshProps().apply(block)
    return Mesh(
        gl,
        props.isStatic,
        props.maxVertices,
        props.maxIndices,
        props.useBatcher,
        props.autoBind,
        attributes
    ).also { it.indicesAsTri() }
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_PACKED] attributes.
 */
fun colorMesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED), generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun colorMeshUnpacked(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_UNPACKED), generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D], [VertexAttribute.COLOR_PACKED], and [VertexAttribute.TEX_COORDS] attributes.
 */
fun textureMesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(
        gl,
        listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED, VertexAttribute.TEX_COORDS(0)),
        generate
    )
}

fun positionMesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D), generate)
}

fun position3Mesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_VEC3), generate)
}

fun position4Mesh(gl: GL, generate: MeshProps.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_VEC4), generate)
}

fun Application.mesh(attributes: List<VertexAttribute>, block: MeshProps.() -> Unit): Mesh {
    return mesh(gl, attributes, block)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun Application.colorMesh(generate: MeshProps.() -> Unit): Mesh {
    return colorMesh(gl, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun Application.colorMeshUnpacked(generate: MeshProps.() -> Unit): Mesh {
    return colorMeshUnpacked(gl, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D], [VertexAttribute.COLOR_PACKED], and [VertexAttribute.TEX_COORDS] attributes.
 */
fun Application.textureMesh(generate: MeshProps.() -> Unit): Mesh {
    return textureMesh(gl, generate)
}

class MeshProps {
    var isStatic: Boolean = true
    private var indicesSpecificallySet = false

    /**
     * Updates [maxIndices] as well if it has not been set.
     */
    var maxVertices = 1000
        set(value) {
            field = value
            if (!indicesSpecificallySet) {
                _maxIndices = floor(field * 1.5f).toInt()
            }
        }
    private var _maxIndices = floor(maxVertices * 1.5f).toInt()
    var maxIndices
        get() = _maxIndices
        set(value) {
            indicesSpecificallySet = true
            _maxIndices = value
        }
    var useBatcher = true
    var autoBind = true
}

class MeshBatcher(size: Int, val attributes: VertexAttributes) {
    val vertexSize = attributes.sumOf { if (it == VertexAttribute.COLOR_PACKED) 1 else it.numComponents }
    val vertices = FloatArray(size * vertexSize)
    var lastCount = 0
        private set
    var count = 0
        private set
    private var offset = 0

    fun setVertex(props: VertexProps) {
        attributes.forEach { vertexAttribute ->
            when (vertexAttribute.usage) {
                VertexAttrUsage.POSITION -> {
                    vertices[offset++] = props.x
                    vertices[offset++] = props.y
                    if (vertexAttribute.numComponents >= 3) {
                        vertices[offset++] = props.z
                    }
                    if (vertexAttribute.numComponents == 4) {
                        vertices[offset++] = props.w
                    }
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
        count++
    }

    fun reset() {
        lastCount = count
        offset = 0
        count = 0
    }

}

/**
 * Vertex props that can be used when setting a vertex on a [Mesh].
 * @see [Mesh.setVertex]
 */
class VertexProps {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var w: Float = 0f
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
    var useBatcher: Boolean = true,
    var autoBind: Boolean = true,
    attributes: List<VertexAttribute>
) : Disposable {

    private val vertexAttributes = VertexAttributes(attributes)
    private val vertices = VertexBufferObject(gl, isStatic, maxVertices, vertexAttributes)
    private val indices = IndexBufferObject(gl, maxIndices, isStatic)
    private var updateVertices = false
    private val tempVertexProps = VertexProps()

    /**
     * The batcher allows you to [setVertex] without having to worry about creating the data array and positioning
     * of the values yourself. The batcher will also keep count of each vertex for rendering purposes.
     * @see [verticesPerIndex]
     */
    val batcher = MeshBatcher(maxVertices, vertexAttributes)

    /**
     * The number of vertices shared per index. If you are drawing just a triangle, each vertex would only have 1 index.
     * If you wanted to draw a quad (4 vertices), you would use 6 indices. Each index would share 1.5 vertices.
     *
     * If you are setting your own indices then you will want to change this value to match that data.
     * **WARNING**: A call to [indicesAsTri] or [indicesAsQuad] will alter this value!
     *
     * Defaults to **1**.
     */
    var verticesPerIndex = 1f

    val verticesBuffer get() = vertices.buffer
    val indicesBuffer get() = indices.buffer

    val numIndices get() = indices.numIndices
    val numVertices get() = vertices.numVertices

    /**
     * The default total count for rendnering vertices.
     * If using [batcher], this will default to the `batch.count * verticesPerIndex`
     * If not using the [batcher], this will default to the [numIndices], if greater than 0, else the [numVertices]
     *
     * @see MeshBatcher.count
     * @see verticesPerIndex
     */
    val defaultCount: Int
        get() {
            return if (useBatcher) {
                if (batcher.count > 0) {
                    floor(batcher.count * verticesPerIndex).toInt()
                } else {
                    floor(batcher.lastCount * verticesPerIndex).toInt()
                }
            } else if (numIndices > 0) {
                numIndices
            } else {
                numVertices
            }
        }

    /**
     * Sets a vertex based on the [VertexProps]. If the mesh created is missing a [VertexAttribute], setting the value
     * of that attribute will do nothing. E.g. Creating a mesh with only a [VertexAttribute.POSITION_2D] and setting the
     * [VertexProps.colorPacked] field will do nothing.
     */
    fun setVertex(action: VertexProps.() -> Unit) {
        if (!useBatcher) throw RuntimeException("This mesh isn't user the mesh batcher! You cannot use setVertex. You must pass in a FloatArray to setVertices.")
        tempVertexProps.action()
        batcher.setVertex(tempVertexProps)
        updateVertices = true
        resetProps()
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

    /**
     * Creates an array of indices based on a quad and uploads the indices to the [IndexBufferObject].
     * This will set [verticesPerIndex] to **1.5**.
     */
    fun indicesAsQuad() {
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
        verticesPerIndex = 1.5f
    }

    /**
     * Creates an array of indices based on a triangle and uploads the indices to the [IndexBufferObject].
     * This will set [verticesPerIndex] to **1**.
     */
    fun indicesAsTri() {
        val indices = ShortArray(maxIndices)
        for (i in 0 until maxIndices step 3) {
            indices[i] = i.toShort()
            indices[i + 1] = (i + 1).toShort()
            indices[i + 2] = (i + 2).toShort()
        }
        setIndices(indices)
        verticesPerIndex = 1f
    }

    /**
     * Sets the vertices of this mesh directly. Updates the [VertexBufferObject].
     */
    fun setVertices(vertices: FloatArray, srcOffset: Int = 0, count: Int = vertices.size): Mesh {
        this.vertices.setVertices(vertices, srcOffset, count)
        return this
    }

    /**
     * Updates a portion of the [VertexBufferObject] vertices.
     */
    fun updateVertices(destOffset: Int, source: FloatArray, srcOffset: Int = 0, count: Int = source.size): Mesh {
        this.vertices.updateVertices(destOffset, source, srcOffset, count)
        return this
    }

    /**
     * Sets indices of this mesh directly. Updtes the [IndexBufferObject].
     */
    fun setIndices(indices: ShortArray, srcOffset: Int = 0, count: Int = indices.size): Mesh {
        this.indices.setIndices(indices, srcOffset, count)
        return this
    }

    /**
     * Binds the [VertexBufferObject] and will also bind the [IndexBufferObject] if the [numIndices] > 0.
     */
    fun bind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        vertices.bind(shader, locations)
        if (numIndices > 0) {
            indices.bind()
        }
    }

    /**
     * Unbinds the [VertexBufferObject] and the [IndexBufferObject] if applicable.
     */
    fun unbind(shader: ShaderProgram<*, *>? = null, locations: IntArray? = null) {
        vertices.unbind(shader, locations)
        if (numIndices > 0) {
            indices.unbind()
        }
    }

    /**
     * Renders the mesh.
     * @param shader the [ShaderProgram] to use with this mesh. Defaults to `null`.
     * @param drawMode the [DrawMode] type. Defaults to [DrawMode.TRIANGLES]
     * @param offset the offset of the current vertices indices to render. Defaults to `0`.
     * @param count the total vertices to render. See [defaultCount]
     */
    fun render(
        shader: ShaderProgram<*, *>? = null,
        drawMode: DrawMode = DrawMode.TRIANGLES,
        offset: Int = 0,
        count: Int = defaultCount,
    ) {
        if (useBatcher && updateVertices) {
            setVertices(batcher.vertices)
            if (numIndices > 0) {
                indicesBuffer.apply {
                    position = 0
                    limit = count
                }
            }
            batcher.reset()
            updateVertices = false
        } else if (count == 0) {
            return
        }

        if (autoBind) {
            bind(shader)
        }
        if (numIndices > 0) {
            if (count + offset > indices.maxNumIndices) {
                throw RuntimeException("Mesh attempting to access memory outside of the index buffer (count: $count, offset: $offset, max: $numIndices)")
            }
            gl.drawElements(drawMode, count, IndexType.UNSIGNED_SHORT, offset * 2)
        } else {
            gl.drawArrays(drawMode, offset, count)
        }
        if (autoBind) {
            unbind(shader)
        }
    }

    /**
     * Disposes the mesh and any buffers.
     */
    override fun dispose() {
        vertices.dispose()
        indices.dispose()
    }
}