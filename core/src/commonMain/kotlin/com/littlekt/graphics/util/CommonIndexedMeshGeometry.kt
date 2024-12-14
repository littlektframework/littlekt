package com.littlekt.graphics.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.math.spatial.BoundingBox

/**
 * A [IndexedMeshGeometry] that calculates the [BoundingBox] of the geometry as well as using
 * [CommonVertexView] when adding a vertex.
 *
 * @param layout a [VertexBufferLayout] describing the vertex buffer.
 * @param size the initial size of the vertices and indices buffers
 * @author Colton Daily
 * @date 4/10/2024
 */
class CommonIndexedMeshGeometry(layout: VertexBufferLayout, size: Int = INITIAL_SIZE) :
    IndexedMeshGeometry(layout, size) {
    /** Bounds of the mesh. */
    val bounds = BoundingBox()

    /** The current vertex view of the geometry. */
    val view =
        CommonVertexView(
            layout.attributes.sumOf { it.format.components },
            vertices,
            layout.attributes,
            0,
        )

    /**
     * Mark this geometry as a batch update. This does nothing on its own. Use [isBatchUpdate] to
     * handle.
     */
    inline fun batchUpdate(
        rebuildBounds: Boolean = false,
        block: CommonIndexedMeshGeometry.() -> Unit,
    ) {
        this.batchUpdate(block)
        if (rebuildBounds) {
            rebuildBounds()
        }
    }

    /** Add a single vertex to the geometry using a [VertexView]. */
    inline fun addVertex(action: CommonVertexView.() -> Unit): Int {
        val result = this.addVertex(view, action)
        bounds.add(view.position)
        return result
    }

    /** Clears the current bounds and rebuilds the bounds based on the position vertices. */
    fun rebuildBounds() {
        bounds.clear()
        for (i in 0 until numVertices) {
            view.index = i
            bounds.add(view.position)
        }
    }

    /**
     * Create a new [CommonVertexView] at the specified index.
     *
     * @param i the vertex index
     */
    operator fun get(i: Int): CommonVertexView {
        if (i < 0 || i >= vertices.capacity / vertexSize) {
            throw IllegalStateException("Vertex index out of bounds: $i")
        }
        return CommonVertexView(layout.arrayStride.toInt(), vertices, layout.attributes, i)
    }

    /** Iterate through the vertex view by index. */
    inline fun forEach(block: (CommonVertexView) -> Unit) {
        for (i in 0 until numVertices) {
            view.index = i
            block(view)
        }
    }

    companion object {
        private const val INITIAL_SIZE = 1000
    }
}
