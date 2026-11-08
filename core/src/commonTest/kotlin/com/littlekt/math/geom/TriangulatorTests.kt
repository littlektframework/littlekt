package com.littlekt.math.geom

import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.datastructure.ShortArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TriangulatorTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-3f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun polygonArea(v: FloatArray, offset: Int = 0, count: Int = v.size): Float {
        var area = 0f
        val n = count / 2
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += v[offset + i * 2] * v[offset + j * 2 + 1] - v[offset + j * 2] * v[offset + i * 2 + 1]
        }
        return area / 2f
    }

    private fun signedTriangleArea(v: FloatArray, a: Int, b: Int, c: Int): Float {
        val ax = v[a * 2]
        val ay = v[a * 2 + 1]
        val bx = v[b * 2]
        val by = v[b * 2 + 1]
        val cx = v[c * 2]
        val cy = v[c * 2 + 1]
        return ((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) / 2f
    }

    private fun assertValidTriangulation(
        v: FloatArray,
        triangles: ShortArrayList,
        offset: Int = 0,
        count: Int = v.size
    ) {
        val n = count / 2
        val first = offset / 2
        assertEquals((n - 2) * 3, triangles.size)
        var total = 0f
        for (t in 0 until triangles.size step 3) {
            val a = triangles[t].toInt()
            val b = triangles[t + 1].toInt()
            val c = triangles[t + 2].toInt()
            for (i in intArrayOf(a, b, c)) {
                assertTrue(i in first until first + n, "index $i out of range")
            }
            assertTrue(a != b && b != c && a != c)
            val area = signedTriangleArea(v, a, b, c)
            assertTrue(area <= 0f, "triangle $a,$b,$c is not clockwise: $area")
            total += abs(area)
        }
        assertNear(abs(polygonArea(v, offset, count)), total)
    }

    @Test
    fun test_single_triangle() {
        val v = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
        val triangles = Triangulator().computeTriangles(v)
        assertValidTriangulation(v, triangles)
    }

    @Test
    fun test_convex_square_ccw() {
        val v = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        val triangles = Triangulator().computeTriangles(v)
        assertValidTriangulation(v, triangles)
        assertNear(1f, abs(polygonArea(v)))
    }

    @Test
    fun test_convex_square_cw() {
        val v = floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_convex_regular_polygon() {
        val n = 12
        val v = FloatArray(n * 2)
        for (i in 0 until n) {
            val a = i * 2.0 * kotlin.math.PI / n
            v[i * 2] = cos(a).toFloat() * 5f
            v[i * 2 + 1] = sin(a).toFloat() * 5f
        }
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_concave_arrow() {
        val v = floatArrayOf(0f, 0f, 4f, 0f, 4f, 4f, 2f, 1f, 0f, 4f)
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_concave_l_shape() {
        val v = floatArrayOf(0f, 0f, 3f, 0f, 3f, 1f, 1f, 1f, 1f, 3f, 0f, 3f)
        val triangles = Triangulator().computeTriangles(v)
        assertValidTriangulation(v, triangles)
        assertNear(5f, abs(polygonArea(v)))
    }

    @Test
    fun test_concave_l_shape_cw() {
        val v = floatArrayOf(0f, 3f, 1f, 3f, 1f, 1f, 3f, 1f, 3f, 0f, 0f, 0f)
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_concave_star() {
        val n = 10
        val v = FloatArray(n * 2)
        for (i in 0 until n) {
            val a = i * 2.0 * kotlin.math.PI / n
            val r = if (i % 2 == 0) 5f else 2f
            v[i * 2] = cos(a).toFloat() * r
            v[i * 2 + 1] = sin(a).toFloat() * r
        }
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_comb_polygon() {
        val v = floatArrayOf(0f, 0f, 6f, 0f, 6f, 4f, 5f, 4f, 4f, 1f, 3f, 4f, 2f, 1f, 1f, 4f, 0f, 4f)
        assertValidTriangulation(v, Triangulator().computeTriangles(v))
    }

    @Test
    fun test_float_array_list_input() {
        val list = FloatArrayList()
        list.add(floatArrayOf(0f, 0f, 3f, 0f, 3f, 1f, 1f, 1f, 1f, 3f, 0f, 3f))
        val triangles = Triangulator().computeTriangles(list)
        assertValidTriangulation(list.data, triangles, 0, list.size)
    }

    @Test
    fun test_offset_and_count() {
        val v = floatArrayOf(9f, 9f, 9f, 9f, 0f, 0f, 3f, 0f, 3f, 1f, 1f, 1f, 1f, 3f, 0f, 3f, 9f, 9f)
        val triangles = Triangulator().computeTriangles(v, 4, 12)
        assertValidTriangulation(v, triangles, 4, 12)
    }

    @Test
    fun test_reusing_triangulator() {
        val triangulator = Triangulator()
        val big = floatArrayOf(0f, 0f, 3f, 0f, 3f, 1f, 1f, 1f, 1f, 3f, 0f, 3f)
        assertValidTriangulation(big, triangulator.computeTriangles(big))
        val small = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        assertValidTriangulation(small, triangulator.computeTriangles(small))
        assertValidTriangulation(big, triangulator.computeTriangles(big))
    }
}
