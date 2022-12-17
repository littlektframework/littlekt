package com.lehaine.littlekt.graphics.g2d.shape

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.PI2_F
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.math.isFuzzyEqual

internal object Joiner {
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