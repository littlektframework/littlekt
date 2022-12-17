package com.lehaine.littlekt.graphics.g2d.shape

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.PI2_F
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.*
import com.lehaine.littlekt.math.isFuzzyEqual
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

internal class PolygonDrawer(batchManager: BatchManager, private val lineDrawer: LineDrawer) : Drawer(batchManager) {

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
                joinType == JoinType.SMOOTH
            )
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