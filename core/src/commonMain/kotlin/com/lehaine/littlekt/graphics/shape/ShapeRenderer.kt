package com.lehaine.littlekt.graphics.shape

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.Batch.Companion.C1
import com.lehaine.littlekt.graphics.Batch.Companion.C2
import com.lehaine.littlekt.graphics.Batch.Companion.C3
import com.lehaine.littlekt.graphics.Batch.Companion.C4
import com.lehaine.littlekt.graphics.Batch.Companion.U1
import com.lehaine.littlekt.graphics.Batch.Companion.V1
import com.lehaine.littlekt.graphics.Batch.Companion.X1
import com.lehaine.littlekt.graphics.Batch.Companion.X2
import com.lehaine.littlekt.graphics.Batch.Companion.X3
import com.lehaine.littlekt.graphics.Batch.Companion.X4
import com.lehaine.littlekt.graphics.Batch.Companion.Y1
import com.lehaine.littlekt.graphics.Batch.Companion.Y2
import com.lehaine.littlekt.graphics.Batch.Companion.Y3
import com.lehaine.littlekt.graphics.Batch.Companion.Y4
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.*
import kotlin.jvm.JvmStatic
import kotlin.math.*


/**
 * @author Colton Daily
 * @date 7/18/2022
 */
enum class JoinType {
    NONE,
    POINTY,
    SMOOTH
}

/**
 * Renders primitive shapes using an existing [Batch].
 *
 * Ported from [Shape Drawer](https://github.com/earlygrey/shapedrawer) by **earlygrey**.
 * @param batch the batch used to batch draw calls with
 * @param slice a 1x1 slice of a texture. Generally a single white pixel.
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRenderer(val batch: Batch, val slice: TextureSlice = Textures.white) {
    var snap = false
    var thickness: Int = 1
    var sideEstimator: SideEstimator = DefaultSideEstimator()
    val pixelSize: Float get() = batchManager.pixelSize
    var colorBits: Float
        set(value) {
            batchManager.colorBits = value
        }
        get() = batchManager.colorBits

    private val batchManager = BatchManager(batch, slice)
    private val lineDrawer = LineDrawer(batchManager)
    private val polygonDrawer = PolygonDrawer(batchManager, lineDrawer)

    /**
     * Draws a line from point A to point B.
     * @param x starting x-coord
     * @param y starting y-coord
     * @param x2 ending x-coord
     * @param y2 ending y-coord
     * @param color color of the start vertex
     * @param color2 color of the end vertex
     * @param thickness the thickness of the line in pixels
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        color: Color,
        color2: Color = color,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) {
        line(x, y, x2, y2, color.toFloatBits(), color2.toFloatBits(), thickness, snap)
    }

    /**
     * Draws a line from point A to point B.
     * @param x starting x-coord
     * @param y starting y-coord
     * @param x2 ending x-coord
     * @param y2 ending y-coord
     * @param colorBits packed color of the start vertex
     * @param colorBits2 packed color of the end vertex
     * @param thickness the thickness of the line in pixels
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        colorBits: Float = this.colorBits,
        colorBits2: Float = colorBits,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) {
        lineDrawer.line(x, y, x2, y2, thickness, snap, colorBits, colorBits2)
    }

    /**
     * Draws a circle around the specified point with the given radius.
     * @param x center x-coord
     * @param y center y-coord
     * @param radius the radius of the circle
     * @param rotation the rotation of the circle
     * @param thickness the thickness of the outline in pixels
     * @param joinType the type of join, see [JoinType]
     */
    fun circle(
        x: Float,
        y: Float,
        radius: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) {
        ellipse(x, y, radius, radius, rotation, thickness, joinType)
    }

    /**
     * Draws an ellipse around the specified point with the given radius's.
     * @param x center x-coord
     * @param y center y-coord
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the outline in pixels
     * @param joinType the type of join, see [JoinType]
     */
    fun ellipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) {
        polygon(x, y, estimateSidesRequired(rx, ry), rx, ry, rotation, thickness, joinType)
    }

    fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Float = colorBits,
    ) {

        val cBits = colorBits
        colorBits = color
        if (joinType == JoinType.NONE) {
            line(x1, y1, x2, y2, thickness = thickness)
            line(x2, y2, x3, y3, thickness = thickness)
            line(x3, y3, x1, y1, thickness = thickness)
        } else {
            // TODO impl via path drawer
        }
        colorBits = cBits
    }

    fun rectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = JoinType.POINTY,
    ) {
        if (joinType == JoinType.POINTY && rotation.radians.isFuzzyZero()) {
            val halfThickness = 0.5f * thickness
            val nx = x + width
            val ny = y + height
            val caching = batchManager.cachingDraws
            lineDrawer.run {
                pushLine(x + halfThickness, y, nx - halfThickness, y, thickness, false) // bottom
                pushLine(x + halfThickness, ny, nx - halfThickness, ny, thickness, false) // top
                pushLine(x, y - halfThickness, x, ny + halfThickness, thickness, false) // left
                pushLine(nx, y - halfThickness, nx, ny + halfThickness, thickness, false) // right
            }
            if (!caching) {
                batchManager.pushToBatch()
            }
            return
        }
        // TODO impl via path
    }

    fun polygon(
        x: Float,
        y: Float,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
    ) {
        polygonDrawer.polygon(x, y, sides, scaleX, scaleY, rotation, thickness, joinType, 0.radians, PI2_F)
    }


    private fun isJoinNecessary(thickness: Int) = thickness > 3 * batchManager.pixelSize
    private fun isJoinNecessary() = isJoinNecessary(thickness)
    private fun estimateSidesRequired(rx: Float, ry: Float) =
        sideEstimator.estimateSidesRequired(batchManager.pixelSize, rx, ry)
}

private class BatchManager(private val batch: Batch, private val slice: TextureSlice) {

    var colorBits: Float = Color.WHITE.toFloatBits()
    private var verts = FloatArray(2000)
    private var vertexCount = 0
    var offset = 0.001f
    var pixelSize = 1f
    val halfPixelSize get() = pixelSize * 0.5f

    private val verticesArrayIndex: Int get() = VERTEX_SIZE * vertexCount
    private val verticesRemaining: Int get() = (verts.size - QUAD_PUSH_SIZE * vertexCount) / VERTEX_SIZE

    var cachingDraws = false
        private set

    init {
        setTextureSliceUV()
    }

    //fun x1(x1: Float) = vertices

    fun startCaching(): Boolean {
        val wasCaching = cachingDraws
        cachingDraws = true
        return wasCaching
    }

    fun endCaching() {
        cachingDraws = false
        if (vertexCount > 0) {
            pushToBatch()
        }
    }

    fun pushVertex() {
        vertexCount++
    }

    fun pushQuad() {
        vertexCount += 4
    }

    fun pushTriangle() {
        x4(x3())
        y4(y3())
        pushQuad()
    }

    fun ensureSpaceForTriangle() {
        ensureSpace(4)
    }

    fun ensureSpaceForQuad() {
        ensureSpace(4)
    }

    fun ensureSpace(vertices: Int) {
        if (vertices * VERTEX_SIZE > verts.size) {
            increaseCacheSize(vertices * VERTEX_SIZE)
        } else if (verticesRemaining < vertices) {
            pushToBatch()
        }
    }

    fun increaseCacheSize(minSize: Int) {
        pushToBatch()
        var newSize = verts.size
        while (minSize > newSize) {
            newSize *= 2
        }
        verts = FloatArray(newSize)
        setTextureSliceUV()
    }

    fun pushToBatch() {
        if (vertexCount == 0) return
        batch.draw(slice.texture, verts, 0, verticesArrayIndex)
        vertexCount = 0
    }

    fun x1(x1: Float) {
        verts[verticesArrayIndex + X1] = x1
    }

    fun y1(y1: Float) {
        verts[verticesArrayIndex + Y1] = y1
    }

    fun x2(x2: Float) {
        verts[verticesArrayIndex + X2] = x2
    }

    fun y2(y2: Float) {
        verts[verticesArrayIndex + Y2] = y2
    }

    fun x3(x3: Float) {
        verts[verticesArrayIndex + X3] = x3
    }

    fun y3(y3: Float) {
        verts[verticesArrayIndex + Y3] = y3
    }

    fun x4(x4: Float) {
        verts[verticesArrayIndex + X4] = x4
    }

    fun y4(y4: Float) {
        verts[verticesArrayIndex + Y4] = y4
    }

    fun x1(): Float {
        return verts[verticesArrayIndex + X1]
    }

    fun y1(): Float {
        return verts[verticesArrayIndex + Y1]
    }

    fun x2(): Float {
        return verts[verticesArrayIndex + X2]
    }

    fun y2(): Float {
        return verts[verticesArrayIndex + Y2]
    }

    fun x3(): Float {
        return verts[verticesArrayIndex + X3]
    }

    fun y3(): Float {
        return verts[verticesArrayIndex + Y3]
    }

    fun x4(): Float {
        return verts[verticesArrayIndex + X4]
    }

    fun y4(): Float {
        return verts[verticesArrayIndex + Y4]
    }

    fun color1(c: Float) {
        verts[verticesArrayIndex + C1] = c
    }

    fun color2(c: Float) {
        verts[verticesArrayIndex + C2] = c
    }

    fun color3(c: Float) {
        verts[verticesArrayIndex + C3] = c
    }

    fun color4(c: Float) {
        verts[verticesArrayIndex + C4] = c
    }

    private fun setTextureSliceUV() {
        val u = 0.5f * (slice.u + slice.u2)
        val v = 0.5f * (slice.v + slice.v2)
        for (i in verts.indices step VERTEX_SIZE) {
            verts[i + U1] = u
            verts[i + V1] = v
        }
    }

    companion object {
        private const val VERTEX_SIZE = 5
        private const val QUAD_PUSH_SIZE = 4 * VERTEX_SIZE
    }
}

private open class Drawer(protected val batchManager: BatchManager) {

    fun snapPixel(a: Float, pixelSize: Float, halfPixelSize: Float) =
        (round(a / pixelSize) * pixelSize) + halfPixelSize

    fun drawSmoothJoinFill(
        a: MutableVec2f,
        b: MutableVec2f,
        c: MutableVec2f,
        d: MutableVec2f,
        e: MutableVec2f,
        halfThickness: Float,
    ) {
        batchManager.ensureSpaceForTriangle()
        var bendsLeft = Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, false)
        vert1(if (bendsLeft) e else d)
        vert2(if (bendsLeft) d else e)
        bendsLeft = Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, true)
        vert3(if (bendsLeft) e else d)
        val cBits = batchManager.colorBits
        color(cBits, cBits, cBits)
        batchManager.pushTriangle()
    }

    fun drawSmoothJoinFill(
        a: MutableVec2f,
        b: MutableVec2f,
        c: MutableVec2f,
        d: MutableVec2f,
        e: MutableVec2f,
        offset: Vec2f,
        cos: Float,
        sin: Float,
        halfThickness: Float,
    ) {
        batchManager.ensureSpaceForTriangle()
        var bendsLeft = Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, false)
        val v1 = if (bendsLeft) e else d
        val v2 = if (bendsLeft) d else e
        vert1(v1.x * cos - v1.y * sin + offset.x, v1.x * sin + v1.y * cos + offset.y)
        vert2(v2.x * cos - v2.y * sin + offset.x, v2.x * sin + v2.y * cos + offset.y)
        bendsLeft = Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, true)
        val v3 = if (bendsLeft) e else d
        val x = v3.x * cos - v3.y * sin + offset.x
        val y = v3.x * sin + v3.y * cos + offset.y
        vert3(x, y)
        val cBits = batchManager.colorBits
        color(cBits, cBits, cBits)
        batchManager.pushTriangle()
    }

    fun x1(x1: Float) {
        batchManager.x1(x1)
    }

    fun y1(y1: Float) {
        batchManager.y1(y1)
    }

    fun x2(x2: Float) {
        batchManager.x2(x2)
    }

    fun y2(y2: Float) {
        batchManager.y2(y2)
    }

    fun x3(x3: Float) {
        batchManager.x3(x3)
    }

    fun y3(y3: Float) {
        batchManager.y3(y3)
    }

    fun x4(x4: Float) {
        batchManager.x4(x4)
    }

    fun y4(y4: Float) {
        batchManager.y4(y4)
    }

    fun vert1(x: Float, y: Float) {
        x1(x)
        y1(y)
    }

    fun vert2(x: Float, y: Float) {
        x2(x)
        y2(y)
    }

    fun vert3(x: Float, y: Float) {
        x3(x)
        y3(y)
    }

    fun vert4(x: Float, y: Float) {
        x4(x)
        y4(y)
    }

    fun vert1(v: Vec2f) {
        vert1(v.x, v.y)
    }

    fun vert2(v: Vec2f) {
        vert2(v.x, v.y)
    }

    fun vert3(v: Vec2f) {
        vert3(v.x, v.y)
    }

    fun vert4(v: Vec2f) {
        vert4(v.x, v.y)
    }

    fun vert1(v: Vec2f, offset: Vec2f) {
        vert1(v.x + offset.x, v.y + offset.y)
    }

    fun vert2(v: Vec2f, offset: Vec2f) {
        vert2(v.x + offset.x, v.y + offset.y)
    }

    fun vert3(v: Vec2f, offset: Vec2f) {
        vert3(v.x + offset.x, v.y + offset.y)
    }

    fun vert4(v: Vec2f, offset: Vec2f) {
        vert4(v.x + offset.x, v.y + offset.y)
    }

    fun color1(c: Float) {
        batchManager.color1(c)
    }

    fun color2(c: Float) {
        batchManager.color2(c)
    }

    fun color3(c: Float) {
        batchManager.color3(c)
    }

    fun color4(c: Float) {
        batchManager.color4(c)
    }

    fun color(c1: Float, c2: Float, c3: Float) {
        color1(c1)
        color2(c2)
        color3(c3)
    }

    fun color(c1: Float, c2: Float, c3: Float, c4: Float) {
        color1(c1)
        color2(c2)
        color3(c3)
        color4(c4)
    }

    fun x1(): Float {
        return batchManager.x1()
    }

    fun y1(): Float {
        return batchManager.y1()
    }

    fun x2(): Float {
        return batchManager.x2()
    }

    fun y2(): Float {
        return batchManager.y2()
    }

    fun x3(): Float {
        return batchManager.x3()
    }

    fun y3(): Float {
        return batchManager.y3()
    }

    fun x4(): Float {
        return batchManager.x4()
    }

    fun y4(): Float {
        return batchManager.y4()
    }

    companion object {
        @JvmStatic
        protected val dir = MutableVec2f()

        @JvmStatic
        protected val a = MutableVec2f()

        @JvmStatic
        protected val b = MutableVec2f()

        @JvmStatic
        protected val c = MutableVec2f()

        @JvmStatic
        protected val d = MutableVec2f()

        @JvmStatic
        protected val e = MutableVec2f()

        @JvmStatic
        protected val vec1 = MutableVec2f()
    }
}

private class LineDrawer(batchManager: BatchManager) : Drawer(batchManager) {

    fun line(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Int,
        snap: Boolean,
        c1: Float = batchManager.colorBits,
        c2: Float = batchManager.colorBits,
    ) {
        pushLine(x1, y1, x2, y2, thickness, snap, c1, c2)
    }

    @Suppress("NAME_SHADOWING")
    fun pushLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Int,
        snap: Boolean,
        c1: Float = batchManager.colorBits,
        c2: Float = batchManager.colorBits,
    ) {
        batchManager.ensureSpaceForQuad()

        val dx = x2 - x1
        val dy = y2 - y1

        val offset = batchManager.offset
        val pixelSize = batchManager.pixelSize
        val halfPixelSize = batchManager.halfPixelSize

        val x1 = if (snap) snapPixel(x1, pixelSize, halfPixelSize) - sign(dx) * offset else x1
        val y1 = if (snap) snapPixel(y1, pixelSize, halfPixelSize) - sign(dy) * offset else y1
        val x2 = if (snap) snapPixel(x2, pixelSize, halfPixelSize) - sign(dx) * offset else x2
        val y2 = if (snap) snapPixel(y2, pixelSize, halfPixelSize) - sign(dy) * offset else y2

        var px = 0f
        var py = 0f

        if (x1 == x2) {
            px = thickness * 0.5f
        } else if (y1 == y2) {
            py = thickness * 0.5f
        } else {
            val scale = 1f / sqrt(dx * dx + dy * dy) * (thickness * 0.5f)

            px = dy * scale
            py = dx * scale
        }

        x1(x1 + px)
        y1(y1 - py)
        x2(x1 - px)
        y2(y1 + py)
        x3(x2 - px)
        y3(y2 + py)
        x4(x2 + px)
        y4(y2 - py)

        color1(c1)
        color2(c1)
        color3(c2)
        color4(c2)

        batchManager.pushQuad()
        if (!batchManager.cachingDraws) {
            batchManager.pushToBatch()
        }
    }
}

private class PolygonDrawer(batchManager: BatchManager, private val lineDrawer: LineDrawer) : Drawer(batchManager) {

    private val center = MutableVec2f()
    private val radius = MutableVec2f()

    fun polygon(
        x: Float,
        y: Float,
        sides: Int,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        thickness: Int,
        joinType: JoinType,
        startAngle: Angle,
        radians: Float,
    ) {
        val minRadians = min(radians, PI2_F)

        center.set(x, y)
        radius.set(scaleX, scaleY)

        val wasCaching = batchManager.startCaching()
        if (joinType == JoinType.NONE) {
            drawPolygonWithNoJoin(center, sides, thickness, rotation, radius, startAngle, minRadians)
        } else {
            drawPolygonWithJoin(center,
                sides,
                thickness * 0.5f,
                rotation,
                radius,
                startAngle,
                minRadians,
                joinType == JoinType.SMOOTH)
        }
        if (!wasCaching) {
            batchManager.endCaching()
        }
    }

    fun drawPolygonWithNoJoin(
        center: Vec2f,
        sides: Int,
        thickness: Int,
        rotation: Angle,
        radius: Vec2f,
        startAngle: Angle,
        radians: Float,
    ) {
        val angleInterval = (PI2_F / sides).radians
        val endAngle = (startAngle.radians + radians).radians
        val cos = angleInterval.cosine
        val sin = angleInterval.sine
        val cosRot = rotation.cosine
        val sinRot = rotation.sine
        val start = ceil(sinRot * (startAngle.radians / PI2_F)).toInt()
        val end = floor(sides * (endAngle.radians / PI2_F)).toInt() + 1

        dir.set(1f, 0f).rotate(min(start * angleInterval.radians, endAngle.radians).radians)
        a.set(1f, 0f).rotate(startAngle).scale(radius)
        b.set(dir).scale(radius)
        for (i in start..end) {
            val x1: Float = a.x * cosRot - a.y * sinRot + center.x
            val y1: Float = a.x * sinRot + a.y * cosRot + center.y
            val x2: Float = b.x * cosRot - b.y * sinRot + center.x
            val y2: Float = b.x * sinRot + b.y * cosRot + center.y
            lineDrawer.pushLine(x1, y1, x2, y2, thickness, false)
            if (i < end - 1) {
                a.set(b)
                dir.set(dir.x * cos - dir.y * sin, dir.x * sin + dir.y * cos)
                b.set(dir).scale(radius)
            } else if (i == end - 1) {
                a.set(b)
                b.set(1f, 0f).rotate(endAngle).scale(radius)
            }
        }
    }

    fun drawPolygonWithJoin(
        center: Vec2f,
        sides: Int,
        halfThickness: Float,
        rotation: Angle,
        radius: Vec2f,
        startAngle: Angle,
        radians: Float,
        smooth: Boolean,
    ) {
        val full = radians.isFuzzyEqual(PI2_F, 0.001f)

        val angleInterval = (PI2_F / sides).radians
        val endAngle = (startAngle.radians + radians).radians
        val cos = angleInterval.cosine
        val sin = angleInterval.sine
        val cosRot = rotation.cosine
        val sinRot = rotation.sine
        var start: Int
        var end: Int
        if (full) {
            start = 1
            end = sides
            dir.set(1f, 0f).rotate(start * angleInterval)
            a.set(1f, 0f).rotate((start - 2) * angleInterval).scale(radius)
            c.set(dir).scale(radius)
            b.set(1f, 0f).rotate((start - 1) * angleInterval).scale(radius)
        } else {
            start = ceil(sides * (startAngle.radians / PI2_F)).toInt()
            if ((start * angleInterval).radians.isFuzzyEqual(startAngle.radians, 0.001f)) {
                start++
            }
            end = floor(sides * (endAngle.radians / PI2_F)).toInt() + 1
            end = min(end, start + sides)
            dir.set(1f, 0f).rotate(min((start * angleInterval).radians, endAngle.radians).radians)
            a.set(1f, 0f).rotate((start - 1) * angleInterval).scale(radius)
            b.set(1f, 0f).rotate(startAngle).scale(radius)
            c.set(dir).scale(radius)
        }

        for (i in start..end) {
            batchManager.ensureSpaceForQuad()

            if (!full && i == start) {
                Joiner.prepareRadialEndpoint(b, d, e, halfThickness)
            } else {
                if (smooth) {
                    Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, true)
                } else {
                    Joiner.preparePointyJoin(a, b, c, d, e, halfThickness)
                }
            }

            vert1(e.x * cosRot - e.y * sinRot + center.x, e.x * sinRot + e.y * cosRot + center.y)
            vert2(d.x * cosRot - d.y * sinRot + center.x, +d.x * sinRot + d.y * cosRot + center.y)

            if (full || i < end) {
                a.set(b)
                b.set(c)
                dir.set(dir.x * cos - dir.y * sin, dir.x * sin + dir.y * cos)
                c.set(dir).scale(radius)
            } else {
                b.set(1f, 0f).rotate(endAngle).scale(radius)
            }

            if (full || i < end) {
                if (smooth) {
                    Joiner.prepareSmoothJoin(a, b, c, d, e, halfThickness, false)
                } else {
                    Joiner.preparePointyJoin(a, b, c, d, e, halfThickness)
                }
            } else {
                Joiner.prepareRadialEndpoint(b, d, e, halfThickness)
            }

            vert3(d.x * cosRot - d.y * sinRot + center.x, d.x * sinRot + d.y * cosRot + center.y)
            vert4(e.x * cosRot - e.y * sinRot + center.x, e.x * sinRot + e.y * cosRot + center.y)

            val cBits = batchManager.colorBits
            color(cBits, cBits, cBits, cBits)
            batchManager.pushQuad()

            if (smooth && (full || i < end)) {
                drawSmoothJoinFill(a, b, c, d, e, center, cosRot, sinRot, halfThickness)
            }
        }
    }
}

private object Joiner {
    private val ab = MutableVec2f()
    private val bc = MutableVec2f()
    private val v = MutableVec2f()

    fun preparePointyJoin(
        a: MutableVec2f,
        b: MutableVec2f,
        c: MutableVec2f,
        d: MutableVec2f,
        e: MutableVec2f,
        halfThickness: Float,
    ): Angle {
        ab.set(b).subtract(a)
        bc.set(c).subtract(b)
        val angle = ab.angleTo(bc)
        if (angle.radians.isFuzzyEqual(0f, 0.001f) || angle.radians.isFuzzyEqual(PI2_F, 0.001f)) {
            prepareStraightJoin(b, d, e, halfThickness)
            return angle
        }
        val len = halfThickness / angle.sine
        ab.setLength(len)
        bc.setLength(len)
        val bendsLeft = angle.radians < 0f
        val insidePoint = if (bendsLeft) d else e
        val outsidePoint = if (bendsLeft) e else d
        insidePoint.set(b).subtract(ab).add(bc)
        outsidePoint.set(b).add(ab).subtract(bc)
        return angle
    }

    fun prepareSmoothJoin(
        a: MutableVec2f,
        b: MutableVec2f,
        c: MutableVec2f,
        d: MutableVec2f,
        e: MutableVec2f,
        halfThickness: Float,
        startOfEdge: Boolean,
    ): Boolean {
        ab.set(b).subtract(a)
        bc.set(c).subtract(b)
        val angle = ab.angleTo(bc)
        if (angle.radians.isFuzzyEqual(0f, 0.001f) || angle.radians.isFuzzyEqual(PI2_F, 0.001f)) {
            prepareStraightJoin(b, d, e, halfThickness)
            return true
        }
        val len = halfThickness / angle.sine
        ab.setLength(len)
        bc.setLength(len)
        val bendsLeft = angle.radians < 0f
        val insidePoint = if (bendsLeft) d else e
        val outsidePoint = if (bendsLeft) e else d
        insidePoint.set(b).subtract(ab).add(bc)
        val edgeDirection = if (startOfEdge) bc else ab
        // rotate edgeDirection PI/2 towards outsidePoint
        if (bendsLeft) {
            v.set(edgeDirection.y, -edgeDirection.x) // rotate PI/2 clockwise
        } else {
            v.set(-edgeDirection.y, edgeDirection.x) // rotate PI/2 counterclockwise
        }
        v.setLength(halfThickness)
        outsidePoint.set(b).add(v)
        return bendsLeft
    }

    fun prepareStraightJoin(b: MutableVec2f, d: MutableVec2f, e: MutableVec2f, halfThickness: Float) {
        ab.setLength(halfThickness)
        d.set(-ab.y, ab.x).add(b)
        e.set(ab.y, -ab.x).add(b)
    }

    fun prepareFlatEndpoint(
        pathPointX: Float,
        pathPointY: Float,
        endPointX: Float,
        endPointY: Float,
        d: MutableVec2f,
        e: MutableVec2f,
        halfThickness: Float,
    ) {
        v.set(endPointX, endPointY).subtract(pathPointX, pathPointY).setLength(halfThickness)
        d.set(v.y, -v.x).add(endPointX, endPointY)
        e.set(-v.y, v.x).add(endPointX, endPointY)
    }

    fun prepareFlatEndpoint(
        pathPoint: Vec2f,
        endPoint: Vec2f,
        d: MutableVec2f,
        e: MutableVec2f,
        halfThickness: Float,
    ) = prepareFlatEndpoint(pathPoint.x, pathPoint.y, endPoint.x, endPoint.y, d, e, halfThickness)

    fun prepareRadialEndpoint(a: MutableVec2f, d: MutableVec2f, e: MutableVec2f, halfThickness: Float) {
        v.set(a).setLength(halfThickness)
        d.set(a).subtract(v)
        e.set(a).add(v)
    }
}