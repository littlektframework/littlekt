package com.lehaine.littlekt.graphics.util

import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.VertexAttributes
import com.lehaine.littlekt.graphics.gl.Usage
import com.lehaine.littlekt.math.spatial.BoundingBox
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import com.lehaine.littlekt.util.datastructure.ShortArrayList
import kotlin.math.floor

/**
 * @author Colton Daily
 * @date 12/19/2022
 */
class MeshGeometry(
    val usage: Usage,
    val attributes: VertexAttributes,
    size: Int = INITIAL_SIZE,
    val grow: Boolean = false,
) {
    val vertexSize = attributes.sumOf { if (it == VertexAttribute.COLOR_PACKED) 1 else it.numComponents }

    internal val vertices = FloatArrayList(size * vertexSize, grow)
    internal val indices = ShortArrayList(floor(size * 1.5f).toInt(), grow)

    var numVertices = 0
    val numIndices: Int
        get() = indices.size

    val dirty: Boolean get() = verticesDirty || indicesDirty
    var verticesDirty = false
    var indicesDirty = false

    val bounds = BoundingBox()

    var isBatchUpdate = false

    private var rebuildIndicesType: IndicesType = IndicesType.UNKNOWN

    @PublishedApi
    internal val view = VertexView(this, 0)

    inline fun batchUpdate(rebuildBounds: Boolean = false, block: MeshGeometry.() -> Unit) {
        val wasBatchUpdate = isBatchUpdate
        isBatchUpdate = true
        block.invoke(this)
        verticesDirty = true
        isBatchUpdate = wasBatchUpdate
        if (rebuildBounds) {
            rebuildBounds()
        }
    }

    inline fun addVertex(action: VertexView.() -> Unit): Int {
        view.index = numVertices++
        view.resetToZero()
        view.action()
        bounds.add(view.position)
        verticesDirty = true
        return numVertices - 1
    }

    fun add(newVertices: FloatArray, srcOffset: Int = 0, dstOffset: Int = 0, count: Int = newVertices.size) {
        newVertices.copyInto(vertices.data, dstOffset, srcOffset, srcOffset + count)
        vertices.size = dstOffset + count
        numVertices = vertices.size / vertexSize
        verticesDirty = true
    }

    fun addIndex(idx: Int) {
        indices += idx.toShort()
        indicesDirty = true
    }

    fun addIndices(vararg indices: Int) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    fun addIndices(indices: List<Int>) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    fun addTriIndices(i0: Int, i1: Int, i2: Int) {
        addIndex(i0)
        addIndex(i1)
        addIndex(i2)
    }

    /**
     * Converts the array of indices based on a quad.
     */
    fun indicesAsQuad() {
        rebuildIndicesType = IndicesType.QUAD
        var i = 0
        var j = 0
        while (i < indices.capacity) {
            indices[i] = j.toShort()
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j.toShort()
            i += 6
            j += 4
        }
        indicesDirty = true
    }

    /**
     * Converts the array of indices based on a triangle.
     */
    fun indicesAsTri() {
        rebuildIndicesType = IndicesType.TRI
        for (i in 0 until indices.capacity step 3) {
            indices[i] = i.toShort()
            indices[i + 1] = (i + 1).toShort()
            indices[i + 2] = (i + 2).toShort()
        }
        indicesDirty = true
    }

    fun rebuildBounds() {
        bounds.clear()
        for (i in 0 until numVertices) {
            view.index = i
            bounds.add(view.position)
        }
    }

    fun skip(totalVertices: Int) {
        vertices.size += totalVertices * vertexSize
        numVertices += totalVertices
    }

    fun clear() {
        numVertices = 0
        vertices.size = 0
    }

    fun clearIndices() {
        indices.size = 0
        indicesDirty = true
    }

    operator fun get(i: Int): VertexView {
        if (i < 0 || i >= vertices.capacity / vertexSize) {
            throw IllegalStateException("Vertex index out of bounds: $i")
        }
        return VertexView(this, i)
    }

    inline fun forEach(block: (VertexView) -> Unit) {
        for (i in 0 until numVertices) {
            view.index = i
            block(view)
        }
    }

    private enum class IndicesType {
        TRI,
        QUAD,
        UNKNOWN
    }

    companion object {
        private const val INITIAL_SIZE = 1000
    }
}