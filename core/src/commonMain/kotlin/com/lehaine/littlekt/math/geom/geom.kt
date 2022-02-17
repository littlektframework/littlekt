package com.lehaine.littlekt.math.geom

import com.lehaine.littlekt.math.FUZZY_EQ_F
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.clamp
import kotlin.math.sqrt

private val d1 = MutableVec2f()
private val d2 = MutableVec2f()
private val r = MutableVec2f()
private val c1 = MutableVec2f()
private val c2 = MutableVec2f()
private val temp2 = MutableVec2f()

fun closestPointsBetweenSegments(p1: Vec2f, q1: Vec2f, p2: Vec2f, q2: Vec2f): Float {
    q1.subtract(p1, d1) // direction vector of segment S1.
    q2.subtract(p2, d2) // direction vector of segment S2.
    p1.subtract(p2, r)
    val a = d1.dot(d1) // squared length of segment S1, always non-negative.
    val e = d2.dot(d2) // squared length of segment S2, always non-negative.
    val f = d2.dot(r)
    var s: Float
    var t: Float

    // check if either or both segments degenerate into points.
    if (a <= FUZZY_EQ_F && e <= FUZZY_EQ_F) {
        // both segments degenerate into points.
        return sqrt(r.dot(r))
    }

    if (a <= FUZZY_EQ_F) {
        // first segment degenerates into a point.
        s = 0f
        t = f / e // s = 0 => t = (b*s + f) / e = f / e
        t = t.clamp(0f, 1f)
    } else {
        val c = d1.dot(r)
        if (e <= FUZZY_EQ_F) {
            // second segment degenerates into a point.
            t = 0f
            s = (-c / a).clamp(0f, 1f) // t = 0 => s = (b*t - c) / a = -c / a
        } else {
            // the general non-degenerate case starts here.
            val b = d1.dot(d2)
            val denom = a * e - b * b  // always non-negative.
            // if segments not parallel, compute closest point on L1 to L2 and
            // clamp to segment S1. else pick arbitrary s (here 0).
            s = if (denom != 0f) {
                ((b * f - c * e) / denom).clamp(0f, 1f)
            } else {
                0f
            }
            // compute point on L2 closest to S1(s) using
            // t = Dot((P1 + D1*s) - P2,D2) / Dot(D2,D2) = (b*s + f) / e
            t = (b * s + f) / e

            // if t in [0,1] done. else clamp t, recompute s for the new value
            // of t using s = Dot((P2 + D2*t) - P1,D1) / Dot(D1,D1)= (t*b - c) / a
            // and clamp s to [0, 1].
            if (t < 0f) {
                t = 0f
                s = (-c / a).clamp(0f, 1f)
            } else if (t > 1f) {
                t = 1f
                s = ((b - c) / a).clamp(0f, 1f)
            }
        }
    }
    c1.set(d1).scale(s).add(p1)
    c2.set(d2).scale(t).add(p2)
    c1.subtract(c2, temp2)
    return sqrt(temp2.dot(temp2))
}