package com.littlekt.graphics.util

import com.littlekt.file.ShortBuffer
import com.littlekt.graphics.VertexBufferLayout
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/**
 * Holds vertex and index data that can be used to render to a mesh.
 *
 * @param layout a [VertexBufferLayout] describing the vertex buffer.
 * @param size the initial size of the vertices and indices buffers
 * @author Colton Daily
 * @date 12/19/2022
 */
open class IndexedMeshGeometry(layout: VertexBufferLayout, size: Int = INITIAL_SIZE) :
    MeshGeometry(layout, size) {
    /** The indices buffer. */
    var indices = ShortBuffer(floor(size * 1.5f).toInt())
        private set

    /** The number of indices added to the [indices] buffer. */
    var numIndices: Int = 0
        private set

    /** If the geometry has changed since last update. */
    override val dirty: Boolean
        get() = verticesDirty || indicesDirty

    /** Determines if the [indices] buffer has changed since this value was last false. */
    var indicesDirty = false

    /**
     * The [IndicesType], if using [indicesAsQuad] or [indicesAsTri]. Defaults to
     * [IndicesType.UNKNOWN].
     */
    var indicesType: IndicesType = IndicesType.UNKNOWN
        protected set

    /** Add an index. */
    fun addIndex(idx: Int) {
        ensureIndices()
        numIndices++
        indices += idx.toShort()
        indicesDirty = true
    }

    /** Adds a list of indices. */
    fun addIndices(vararg indices: Int) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    /** Adds a list of indices. */
    fun addIndices(indices: List<Int>) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    /** Adds three indices as a "tri". */
    fun addTriIndices(i0: Int, i1: Int, i2: Int) {
        addIndex(i0)
        addIndex(i1)
        addIndex(i2)
    }

    /**
     * Converts the array of indices based on a quad. Updates [indicesType] to a [IndicesType.QUAD].
     */
    fun indicesAsQuad() {
        indicesType = IndicesType.QUAD
        var i = 0
        var j = 0
        val capacity = indices.capacity
        while (i < capacity) {
            indices[i] = j.toShort()
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j.toShort()
            i += 6 // num indices per quad (3 per tri)
            j += 4 // 4 points per quad
        }
        indicesDirty = true
        numIndices = capacity
        indices.position = capacity
    }

    /**
     * Converts the array of indices based on a triangle. Updates [indicesType] to a
     * [IndicesType.TRI].
     */
    fun indicesAsTri() {
        indicesType = IndicesType.TRI
        val capacity = indices.capacity
        for (i in 0 until capacity step 3) {
            indices[i] = i.toShort()
            indices[i + 1] = (i + 1).toShort()
            indices[i + 2] = (i + 2).toShort()
        }
        indicesDirty = true
        numIndices = capacity
        indices.position = capacity
    }

    /** Clears the indices. */
    fun clearIndices() {
        numVertices = 0
        indices.clear()
        indicesDirty = true
    }

    /**
     * Ensures the buffer has enough for the required amount of vertices. Will ensure indices meets
     * the new vertex size as well.
     *
     * @param required the amount of vertices required (not the stride)
     * @return true if buffers were grown; false otherwise.
     */
    override fun ensureVertices(required: Int): Boolean {
        if (super.ensureVertices(required)) {
            ensureIndices(((vertices.capacity / vertexSize) * 1.5f).toInt())
            return true
        }
        return false
    }

    /**
     * Ensures the buffer has enough for the required amount of indices.
     *
     * @param required the amount of indices required
     * @return true if buffers were grown; false otherwise.
     */
    fun ensureIndices(required: Int = 1): Boolean {
        if (indices.capacity - numIndices < required) {
            increaseIndices(
                max(round(indices.capacity * GROW_FACTOR).toInt(), numIndices + required)
            )
            return true
        }
        return false
    }

    private fun increaseIndices(newSize: Int) {
        logger.debug { "Increasing indices buffer size to $newSize" }
        val oldPosition = indices.position
        indices.position = 0
        val newData = ShortBuffer(newSize)
        newData.put(indices)
        indices = newData
        indices.position = oldPosition
    }

    /** The type of shape indices buffer are used for. */
    enum class IndicesType {
        TRI,
        QUAD,
        UNKNOWN,
    }

    companion object {
        private const val INITIAL_SIZE = 1000
        private const val GROW_FACTOR = 2f
    }
}
