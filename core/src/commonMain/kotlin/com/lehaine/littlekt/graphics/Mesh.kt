package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.IndexType
import com.lehaine.littlekt.graphics.gl.Usage
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.util.MeshBuilder
import com.lehaine.littlekt.graphics.util.MeshGeometry

/**
 * @author Colton Daily
 * @date 11/18/2021
 */

inline fun mesh(
    gl: GL,
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    grow: Boolean = false,
    generate: Mesh.() -> Unit,
): Mesh {
    val mesh = Mesh(
        gl, MeshGeometry(Usage.DYNAMIC_DRAW, VertexAttributes(attributes), size, grow)
    )
    mesh.geometry.indicesAsTri()
    mesh.generate()
    return mesh
}


/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_PACKED] attributes.
 */
fun colorMesh(gl: GL, size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED), size, grow, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun colorMeshUnpacked(gl: GL, size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_UNPACKED), size, grow, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D], [VertexAttribute.COLOR_PACKED], and [VertexAttribute.TEX_COORDS] attributes.
 */
fun textureMesh(gl: GL, size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return mesh(
        gl,
        listOf(VertexAttribute.POSITION_2D, VertexAttribute.COLOR_PACKED, VertexAttribute.TEX_COORDS(0)),
        size,
        grow,
        generate
    )
}

fun positionMesh(gl: GL, size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return mesh(gl, listOf(VertexAttribute.POSITION_2D), size, grow, generate)
}

fun <T : ContextListener> T.mesh(
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    grow: Boolean = false,
    generate: Mesh.() -> Unit,
): Mesh {
    return mesh(context.gl, attributes, size, grow, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun <T : ContextListener> T.colorMesh(size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return colorMesh(context.gl, size, grow, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D] and [VertexAttribute.COLOR_UNPACKED] attributes.
 */
fun <T : ContextListener> T.colorMeshUnpacked(
    size: Int = 1000,
    grow: Boolean = false,
    generate: Mesh.() -> Unit,
): Mesh {
    return colorMeshUnpacked(context.gl, size, grow, generate)
}

/**
 * Creates a mesh with [VertexAttribute.POSITION_2D], [VertexAttribute.COLOR_PACKED], and [VertexAttribute.TEX_COORDS] attributes.
 */
fun <T : ContextListener> T.textureMesh(size: Int = 1000, grow: Boolean = false, generate: Mesh.() -> Unit): Mesh {
    return textureMesh(context.gl, size, grow, generate)
}

class Mesh(
    val gl: GL,
    val geometry: MeshGeometry,
    var autoBind: Boolean = true,
) : Disposable {

    private val isStatic get() = geometry.usage == Usage.STATIC_DRAW
    private val vertexAttributes = geometry.attributes
    val vertices =
        VertexBufferObject(gl, isStatic, geometry.vertices.capacity / geometry.vertexSize, vertexAttributes).apply {
            grow = geometry.grow
        }
    val indices = IndexBufferObject(gl, isStatic, geometry.indices.capacity).apply { grow = geometry.grow }

    val numIndices get() = geometry.numIndices
    val numVertices get() = geometry.numVertices

    /**
     * The default total count for rendering vertices.
     * This will default to the [numIndices], if greater than 0, else the [numVertices]
     */
    val defaultCount: Int
        get() {
            return if (numIndices > 0) {
                numIndices
            } else {
                numVertices
            }
        }

    fun generate(generator: MeshBuilder.() -> Unit) {
        geometry.batchUpdate {
            clear()
            MeshBuilder(this).generator()
        }
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
        if (geometry.dirty) {
            if (geometry.verticesDirty) {
                vertices.setVertices(geometry.vertices.data, 0, geometry.vertices.size)
                geometry.verticesDirty = false
            }
            if (geometry.indicesDirty) {
                indices.setIndices(geometry.indices.data, 0, geometry.indices.size)
                geometry.indicesDirty = false
            }
            if (numIndices > 0) {
                indices.buffer.apply {
                    position = 0
                    limit = count
                }
            }
            geometry.clear()
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