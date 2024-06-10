package com.littlekt.math.geom

import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.datastructure.IntArrayList
import com.littlekt.util.datastructure.ShortArrayList
import kotlin.math.max
import kotlin.math.sign

/**
 * A simple implementation of the ear cutting algorithm to triangulate simple polygons without
 * holes.
 *
 * If the input polygon is not simple (self-intersects), there will be output but it is of
 * unspecified quality (garbage in, garbage out).
 *
 * If the polygon vertices are very large or very close together then [isClockwise] may not be able
 * to properly assess the winding (because it uses floats). In that case the vertices should be
 * adjusted, eg by finding the smallest X and Y values and subtracting that from each vertex.
 *
 * Ported from:
 * [EarClippingTriangulator](https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/EarClippingTriangulator.java)
 *
 * @author Colton Daily
 * @date 7/19/2022
 */
class Triangulator {

    private val indicesArray = ShortArrayList()
    private var indices = ShortArray(0)
    private var vertices = FloatArray(0)
    private var vertexCount = 0
    private val vertexTypes = IntArrayList()
    private val triangles = ShortArrayList()

    /**
     * Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
     *
     * @param vertices pairs describing vertices of the polygon, in either clockwise or
     *   counterclockwise order.
     * @return triples of triangle indices in clockwise order. Note the returned array is reused for
     *   later calls to the same method.
     */
    fun computeTriangles(vertices: FloatArrayList) =
        computeTriangles(vertices.data, 0, vertices.size)

    /**
     * Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
     *
     * @param vertices pairs describing vertices of the polygon, in either clockwise or
     *   counterclockwise order.
     * @return triples of triangle indices in clockwise order. Note the returned array is reused for
     *   later calls to the same method.
     */
    fun computeTriangles(
        vertices: FloatArray,
        offset: Int = 0,
        count: Int = vertices.size
    ): ShortArrayList {
        this.vertices = vertices
        vertexCount = count / 2
        val vertexOffset = offset / 2

        indicesArray.clear()
        indicesArray.ensure(vertexCount)
        indicesArray.size = vertexCount
        indices = indicesArray.data
        if (isClockwise(vertices, offset, count)) {
            for (i in 0 until vertexCount) {
                indices[i] = (vertexOffset + i).toShort()
            }
        } else {
            val n = vertexCount - 1
            for (i in 0 until vertexCount) {
                indices[i] = (vertexOffset + n - i).toShort() // reversed
            }
        }
        vertexTypes.clear()
        vertexTypes.ensure(vertexCount)
        val n = vertexCount
        for (i in 0 until vertexCount) {
            vertexTypes += classifyVertex(i)
        }

        triangles.clear()
        triangles.ensure(max(0, vertexCount - 2) * 3)
        triangulate()
        return triangles
    }

    private fun classifyVertex(index: Int): Int {
        val previous = indices[index.previousIndex] * 2
        val current = indices[index] * 2
        val next = indices[index.nextIndex] * 2
        return computeSpannedAreaSign(
            vertices[previous],
            vertices[previous + 1],
            vertices[current],
            vertices[current + 1],
            vertices[next],
            vertices[next + 1]
        )
    }

    private fun triangulate() {
        val vertexTypes = vertexTypes.data

        while (vertexCount > 3) {
            val earTipIndex = findEarTip()
            cutEarTip(earTipIndex)

            val previousIndex = earTipIndex.previousIndex
            val nextIndex = if (earTipIndex == vertexCount) 0 else earTipIndex
            vertexTypes[previousIndex] = classifyVertex(previousIndex)
            vertexTypes[nextIndex] = classifyVertex(nextIndex)
        }

        if (vertexCount == 3) {
            triangles += indices[0]
            triangles += indices[1]
            triangles += indices[2]
        }
    }

    private fun findEarTip(): Int {
        for (i in 0 until vertexCount) {
            if (isEarTip(i)) return i
        }

        // desperate mode: if no vertex is an ear tip, we are dealing with a degenerate polygon
        // (e.g. nearly collinear).
        // note that the input was not necessarily degenerate, but we could have made it so by
        // clipping some valid ears.

        // idea taken from Martin Held, "FIST: Fast industrial-strength triangulation of polygons",
        // Algorithmica (1998),
        // http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.115.291

        // return a convex or tangential vertex if one exists.
        val vertexTypes = vertexTypes.data
        for (i in 0 until vertexCount) {
            if (vertexTypes[i] != CONCAVE) return i
        }
        // if all vertices are concave, just return the first one.
        return 0
    }

    private fun isEarTip(earTipIndex: Int): Boolean {
        val vertexTypes = vertexTypes.data
        if (vertexTypes[earTipIndex] == CONCAVE) return false

        val prevIndex = earTipIndex.previousIndex
        val nextIndex = earTipIndex.nextIndex
        val p1 = indices[prevIndex] * 2
        val p2 = indices[earTipIndex] * 2
        val p3 = indices[nextIndex] * 2
        val p1x = vertices[p1]
        val p1y = vertices[p1 + 1]
        val p2x = vertices[p2]
        val p2y = vertices[p2 + 1]
        val p3x = vertices[p3]
        val p3y = vertices[p3 + 1]

        // check if any point is inside the triangle formed by previous, current and next vertices.
        // only consider vertices that are not part of this triangle, or else we'll always find one
        // inside.

        var i = nextIndex.nextIndex
        while (i != prevIndex) {
            // concave vertices can obviously be inside the candidate ear, but so can tangential
            // vertices
            // if they coincide with one of the triangle's vertices.
            if (vertexTypes[i] != CONVEX) {
                val v = indices[i] * 2
                val vx = vertices[v]
                val vy = vertices[v + 1]
                // because the polygon has clockwise winding order, the area sign will be positive
                // if the point is strictly inside.
                // it will be 0 on the edge, which we want to include as well.
                // note: check the edge defined by p1->p3 first since this fails _far_ more than the
                // other 2 checks.
                if (computeSpannedAreaSign(p3x, p3y, p1x, p1y, vx, vy) >= 0) {
                    if (computeSpannedAreaSign(p1x, p1y, p2x, p2y, vx, vy) >= 0) {
                        if (computeSpannedAreaSign(p2x, p2y, p3x, p3y, vx, vy) >= 0) return false
                    }
                }
            }
            i = i.nextIndex
        }
        return true
    }

    private fun cutEarTip(earTipIndex: Int) {
        triangles += indices[earTipIndex.previousIndex]
        triangles += indices[earTipIndex]
        triangles += indices[earTipIndex.nextIndex]

        indicesArray.removeAt(earTipIndex)
        vertexTypes.removeAt(earTipIndex)
        vertexCount--
    }

    private val Int.previousIndex: Int
        get() = (if (this == 0) vertexCount else this) - 1

    private val Int.nextIndex: Int
        get() = (this + 1) % vertexCount

    private fun computeSpannedAreaSign(
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        p3x: Float,
        p3y: Float
    ): Int {
        var area = p1x * (p3y - p2y)
        area += p2x * (p1y - p3y)
        area += p3x * (p2y - p1y)
        return sign(area).toInt()
    }

    companion object {
        private val CONCAVE = -1
        private val CONVEX = 1
    }
}
