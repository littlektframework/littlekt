package com.littlekt.graphics.util

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.calculateComponents
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.Vec4f
import com.littlekt.math.spatial.BoundingBox
import kotlin.math.sqrt

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
        CommonVertexView(layout.attributes.calculateComponents(), vertices, layout.attributes, 0)

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

    fun generateNormals() {
        if (!layout.attributes.any { it.usage == VertexAttrUsage.NORMAL }) {
            logger.warn {
                "Attempting to generate normals on geometry that doesn't contain a tangent Attribute!"
            }
            return
        }
        val normals = MutableVec3f()
        val edge1 = MutableVec3f()
        val edge2 = MutableVec3f()
        val v1 = this[0]
        val v2 = this[0]
        val v3 = this[0]
        for (i in 0 until numIndices step 3) {
            v1.index = indices[i].toInt()
            v2.index = indices[i + 1].toInt()
            v3.index = indices[i + 2].toInt()
            v2.position.subtract(v1.position, edge1).norm()
            v3.position.subtract(v1.position, edge2).norm()
            val area = triArea(v1.position, v2.position, v3.position)
            edge1.cross(edge2, normals).norm().scale(area)
            v1.normal.add(normals)
            v2.normal.add(normals)
            v3.normal.add(normals)
        }

        for (i in 0 until numVertices) {
            v1.index = i
            v1.normal.norm()
        }
    }

    private fun triArea(va: Vec3f, vb: Vec3f, vc: Vec3f): Float {
        val xAB = vb.x - va.x
        val yAB = vb.y - va.y
        val zAB = vb.z - va.z
        val xAC = vc.x - va.x
        val yAC = vc.y - va.y
        val zAC = vc.z - va.z
        val abSqr = xAB * xAB + yAB * yAB + zAB * zAB
        val acSqr = xAC * xAC + yAC * yAC + zAC * zAC
        val abcSqr = xAB * xAC + yAB * yAC + zAB * zAC
        return 0.5f * sqrt(abSqr * acSqr - abcSqr * abcSqr)
    }

    /**
     * Calculate the tangent vector if [layout] contains an attribute with the usage of
     * [VertexAttrUsage.TANGENT].
     */
    fun generateTangents(sign: Float = 1f) {
        if (!layout.attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
            logger.warn {
                "Attempting to generate tangents on geometry that doesn't contain a tangent Attribute!"
            }
            return
        }
        val tangents = MutableVec3f()
        val edge1 = MutableVec3f()
        val edge2 = MutableVec3f()
        val v1 = this[0]
        val v2 = this[1]
        val v3 = this[2]

        for (i in 0 until numVertices) {
            v1.index = i
            v1.tangent.set(Vec3f.ZERO)
        }

        for (i in 0 until numIndices step 3) {
            v1.index = indices[i].toInt()
            v2.index = indices[i + 1].toInt()
            v3.index = indices[i + 2].toInt()

            v2.position.subtract(v1.position, edge1)
            v3.position.subtract(v1.position, edge2)

            val du1 = v2.uv.x - v1.uv.x
            val dv1 = v2.uv.y - v1.uv.y
            val du2 = v3.uv.x - v1.uv.x
            val dv2 = v3.uv.y - v1.uv.y

            val f = 1f / (du1 * dv2 - dv1 * du2)
            tangents.set(
                f * (dv2 * edge1.x - dv1 * edge2.x),
                f * (dv2 * edge1.y - dv1 * edge2.y),
                f * (dv2 * edge1.z - dv1 * edge2.z),
            )
            v1.tangent += Vec4f(tangents, 0f)
            v2.tangent += Vec4f(tangents, 0f)
            v3.tangent += Vec4f(tangents, 0f)
        }

        for (i in 0 until numVertices) {
            v1.index = i
            if (v1.normal.sqrLength() == 0f) {
                v1.normal.set(Vec3f.Y_AXIS)
            }

            if (v1.tangent.sqrLength() != 0f) {
                v1.tangent.norm()
                v1.tangent.w = sign
            } else {
                v1.tangent.set(Vec3f.X_AXIS)
            }
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
        return CommonVertexView(
            layout.attributes.calculateComponents(),
            vertices,
            layout.attributes,
            i,
        )
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
