package com.lehaine.littlekt.graphics.shape

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import kotlin.jvm.JvmStatic
import kotlin.math.round

internal open class Drawer(protected val batchManager: BatchManager) {

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