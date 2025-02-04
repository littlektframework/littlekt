package com.littlekt.graphics.util

import com.littlekt.file.ByteBuffer
import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.calculateComponents
import com.littlekt.graphics.calculateStride
import com.littlekt.log.Logger
import com.littlekt.math.spatial.BoundingBox
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Holds ONLY vertex data that can be used to render to a mesh. If indexed data is needed, see
 * [IndexedMeshGeometry].
 *
 * @param layout a [VertexBufferLayout] describing the vertex buffer.
 * @param size the initial size of the vertices and indices buffers
 * @author Colton Daily
 * @date 12/19/2022
 */
open class MeshGeometry(val layout: VertexBufferLayout, size: Int = INITIAL_SIZE) {
    /** The number of components in each vertex. */
    val vertexSize = layout.attributes.calculateComponents()

    /** The size of a vertex stride, in bytes. */
    val vertexStride = layout.attributes.calculateStride()

    /** The vertices buffer. */
    var vertices = ByteBuffer(size * vertexStride)
        private set

    /** The number of vertices added to the [vertices] buffer. */
    var numVertices: Int = 0

    /** If the geometry has changed since last update. */
    open val dirty: Boolean
        get() = verticesDirty

    /** Determines if the [vertices] buffer has changed since this value was last false. */
    var verticesDirty = false

    /** `true` if this is mid-batch update. */
    var isBatchUpdate = false

    /** Bounds of the mesh. This must be calculated yourself or by a subclass. */
    val bounds = BoundingBox()

    /**
     * Mark this geometry as a batch update. This does nothing on its own. Use [isBatchUpdate] to
     * handle.
     */
    inline fun <T : MeshGeometry> batchUpdate(block: T.() -> Unit) {
        val wasBatchUpdate = isBatchUpdate
        isBatchUpdate = true
        @Suppress("UNCHECKED_CAST") block.invoke(this as T)
        verticesDirty = true
        isBatchUpdate = wasBatchUpdate
    }

    /** Add a single vertex to the geometry using a [VertexView]. */
    inline fun <T : VertexView> addVertex(view: T, action: T.() -> Unit): Int {
        ensureVertices()
        view.index = numVertices++
        view.vertices = vertices
        view.resetToZero()
        view.action()
        verticesDirty = true
        return numVertices - 1
    }

    /**
     * Add a list of existing vertices to the geometry. This sets [numVertices] based off the data
     * based in [newVertices].
     */
    fun add(
        newVertices: FloatArray,
        srcOffset: Int = 0,
        dstOffset: Int = 0,
        count: Int = newVertices.size - srcOffset,
    ) {
        ensureVertices(count / vertexSize)
        vertices.putFloat(
            data = newVertices,
            dstOffset = dstOffset * Float.SIZE_BYTES,
            srcOffset = srcOffset,
            len = count,
        )
        if (vertices.position < dstOffset + count * Float.SIZE_BYTES) {
            vertices.position = dstOffset + count * Float.SIZE_BYTES
        }
        numVertices =
            min(
                numVertices + (dstOffset + (count * Float.SIZE_BYTES)) / vertexStride,
                vertices.position / vertexStride,
            )
        verticesDirty = true
    }

    /**
     * This moves the current an X amount of vertices from the current vertex index. This takes into
     * account the [vertexSize].
     */
    fun skip(totalVertices: Int) {
        ensureVertices(totalVertices)
        vertices.position += totalVertices * vertexStride
        numVertices += totalVertices
    }

    /** Clears the vertices. */
    fun clearVertices() {
        numVertices = 0
        vertices.clear()
    }

    /**
     * Ensures the buffer has enough for the required amount of vertices.
     *
     * @param required the amount of vertices required (not the stride)
     * @return true, if buffer increased; false otherwise.
     */
    open fun ensureVertices(required: Int = 1): Boolean {
        if (vertices.capacity - numVertices * vertexStride < vertexStride * required) {
            increaseVertices(
                max(
                    round(vertices.capacity * GROW_FACTOR).toInt(),
                    (numVertices + required) * vertexStride,
                )
            )
            return true
        }
        return false
    }

    private fun increaseVertices(newSize: Int) {
        logger.debug { "Increasing vertices buffer size to $newSize" }
        val oldPos = vertices.position
        val newData = ByteBuffer(newSize)
        vertices.position = 0
        newData.putByte(vertices)
        vertices = newData
        vertices.position = oldPos
    }

    companion object {
        private const val INITIAL_SIZE = 1000
        private const val GROW_FACTOR = 2f

        @JvmStatic protected val logger = Logger<MeshGeometry>()
    }
}
