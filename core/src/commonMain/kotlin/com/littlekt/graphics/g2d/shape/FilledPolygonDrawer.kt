package com.littlekt.graphics.g2d.shape

import com.littlekt.graphics.Color
import com.littlekt.math.PI2_F
import com.littlekt.math.geom.*
import com.littlekt.math.isFuzzyEqual
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 7/19/2022
 */
internal class FilledPolygonDrawer(batchManager: BatchManager) : Drawer(batchManager) {

    fun rectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Angle,
        c1: Color = batchManager.color,
        c2: Color = c1,
        c3: Color = c2,
        c4: Color = c3,
    ) {
        val caching = batchManager.cachingDraws
        batchManager.ensureSpaceForQuad()
        val cos = rotation.cosine
        val sin = rotation.sine
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f
        val centerX = x + halfWidth
        val centerY = y + halfHeight
        x1(halfWidth * cos - halfHeight * sin + centerX)
        y1(halfWidth * sin + halfHeight * cos + centerY)
        x2(-halfWidth * cos - halfHeight * sin + centerX)
        y2(-halfWidth * sin + halfHeight * cos + centerY)
        x3(-halfWidth * cos - (-halfHeight * sin) + centerX)
        y3(-halfWidth * sin + (-halfHeight * cos) + centerY)
        x4(halfWidth * cos - (-halfHeight * sin) + centerX)
        y4(halfWidth * sin + (-halfHeight * cos) + centerY)
        color(c1, c2, c3, c4)
        batchManager.pushQuad()
        if (!caching) {
            batchManager.pushToBatch()
        }
    }

    fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        c1: Color,
        c2: Color,
        c3: Color
    ) {
        val caching = batchManager.cachingDraws
        vert1(x1, y1)
        vert2(x2, y2)
        vert3(x3, y3)
        color(c1, c2, c3)
        batchManager.pushTriangle()
        if (!caching) {
            batchManager.pushToBatch()
        }
    }

    fun polygon(
        vertices: FloatArray,
        offset: Int = 0,
        count: Int = vertices.size,
        color: Color = batchManager.color
    ) {
        val triangles = triangulator.computeTriangles(vertices, offset, count)
        polygon(vertices, triangles.data, triangles.size, color)
    }

    fun polygon(
        cx: Float,
        cy: Float,
        sides: Int,
        rx: Float,
        ry: Float,
        rotation: Angle,
        startAngle: Angle,
        radians: Float,
        innerColor: Color,
        outerColor: Color,
    ) {
        if (radians == 0f) return

        val wasCaching = batchManager.startCaching()

        val angleInterval = (PI2_F / sides).radians
        val endAngle = startAngle + radians.radians
        val cos = angleInterval.cosine
        val sin = angleInterval.sine
        val cosRot = rotation.cosine
        val sinRot = rotation.sine

        var start = ceil(sides * (startAngle.radians / PI2_F)).toInt()
        val end = floor(sides * (endAngle.radians / PI2_F)).toInt() + 1

        if (isFuzzyEqual(start * angleInterval.radians, startAngle.radians, 0.001f)) {
            start++
        }
        b.set(1f, 0f).rotate(startAngle).scale(rx, ry)

        val n = end - start + 1

        if (n < 2) {
            // there are no "regular" segments, will never enter loop,
            // so just push the one triangle from start angle to end angle
            batchManager.ensureSpaceForTriangle()
            a.set(1f, 0f).rotate(startAngle).scale(rx, ry)
            b.set(1f, 0f).rotate(endAngle).scale(rx, ry)
            vert1(cx, cy)
            x2(a.x * cosRot - a.y * sinRot + cx)
            y2(a.x * sinRot + a.y * cos + cy)
            x3(b.x * cosRot - b.y * sinRot + cx)
            y3(b.x * sinRot + b.y * cosRot + cy)
            color(innerColor, outerColor, outerColor)
            batchManager.pushTriangle()
        } else {
            // prepare for regular segments
            dir.set(1f, 0f).rotate(min(start * angleInterval.radians, endAngle.radians).radians)
            c.set(dir).scale(rx, ry)
        }

        for (i in 0 until n - 1) {
            a.set(b)
            b.set(c)
            if (i < n - 2) {
                dir.set(dir.x * cos - dir.y * sin, dir.x * sin + dir.y * cos)
                c.set(dir).scale(rx, ry)
            } else {
                c.set(1f, 0f).rotate(endAngle).scale(rx, ry)
            }

            if (i % 2 == 0) {
                // skip every second triangle so that we can draw it as a quad with the next
                // triangle
                batchManager.ensureSpaceForQuad()
                vert1(cx, cy)
                x2(a.x * cosRot - a.y * sinRot + cx)
                y2(a.x * sinRot + a.y * cosRot + cy)
                x3(b.x * cosRot - b.y * sinRot + cx)
                y3(b.x * sinRot + b.y * cosRot + cy)
                x4(c.x * cosRot - c.y * sinRot + cx)
                y4(c.x * sinRot + c.y * cosRot + cy)
                color(innerColor, outerColor, outerColor, outerColor)
                batchManager.pushQuad()
            } else if (i == n - 2) {
                batchManager.ensureSpaceForTriangle()
                c.set(1f, 0f).rotate(endAngle).scale(rx, ry)
                vert1(cx, cy)
                x2(b.x * cosRot - b.y * sinRot + cx)
                y2(b.x * sinRot + b.y * cosRot + cy)
                x3(c.x * cosRot - c.y + sinRot + cx)
                y3(c.x * sinRot + c.y * cosRot + cy)
                color(innerColor, outerColor, outerColor)
                batchManager.pushTriangle()
            }
        }

        if (!wasCaching) {
            batchManager.endCaching()
        }
    }

    fun polygon(
        vertices: FloatArray,
        triangles: ShortArray,
        trianglesCount: Int = triangles.size,
        color: Color = batchManager.color
    ) {
        for (i in 0 until trianglesCount step 3) {
            batchManager.ensureSpaceForTriangle()
            vert1(vertices[2 * triangles[i]], vertices[2 * triangles[i] + 1])
            vert2(vertices[2 * triangles[i + 1]], vertices[2 * triangles[i + 1] + 1])
            vert3(vertices[2 * triangles[i + 2]], vertices[2 * triangles[i + 2] + 1])
            color(color, color, color)
            batchManager.pushTriangle()
        }
        batchManager.pushToBatch()
    }

    companion object {
        private val triangulator = Triangulator()
    }
}
