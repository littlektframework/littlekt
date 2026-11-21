package com.littlekt.graphics.g3d

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.math.spatial.Capsule
import com.littlekt.math.spatial.Collision
import com.littlekt.math.spatial.SweepHit

/**
 * A sphere or capsule overlap or sweep against [Node3D]s.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
internal class ShapeQuery {
    var isCapsule = false
        private set

    val center = MutableVec3f()
    var radius = 0f
        private set

    val capsule = Capsule()
    val delta = MutableVec3f()
    val bounds = BoundingBox()

    fun setSphere(center: Vec3f, radius: Float, delta: Vec3f = Vec3f.ZERO): ShapeQuery {
        isCapsule = false
        this.center.set(center)
        this.radius = radius
        this.delta.set(delta)
        Collision.sweepBounds(center, radius, delta, bounds)
        return this
    }

    fun setCapsule(capsule: Capsule, delta: Vec3f = Vec3f.ZERO): ShapeQuery {
        isCapsule = true
        this.capsule.set(capsule.a, capsule.b, capsule.radius)
        this.delta.set(delta)
        this.capsule.sweepBounds(delta, bounds)
        return this
    }

    fun overlapsBox(box: BoundingBox): Boolean =
        if (isCapsule) Collision.overlapCapsuleBox(capsule, box)
        else Collision.overlapSphereBox(center, radius, box)

    fun overlapsTriangle(a: Vec3f, b: Vec3f, c: Vec3f): Boolean =
        if (isCapsule) Collision.overlapCapsuleTriangle(capsule, a, b, c)
        else Collision.overlapSphereTriangle(center, radius, a, b, c)

    fun sweepBox(box: BoundingBox, hit: SweepHit): Boolean =
        if (isCapsule) Collision.sweepCapsuleBox(capsule, delta, box, hit)
        else Collision.sweepSphereBox(center, radius, delta, box, hit)

    fun sweepTriangle(a: Vec3f, b: Vec3f, c: Vec3f, hit: SweepHit): Boolean =
        if (isCapsule) Collision.sweepCapsuleTriangle(capsule, delta, a, b, c, hit)
        else Collision.sweepSphereTriangle(center, radius, delta, a, b, c, hit)

    /** @return `false` if the triangle can't be touched, from its bounds */
    fun mayTouch(a: Vec3f, b: Vec3f, c: Vec3f): Boolean {
        val min = bounds.min
        val max = bounds.max
        return !(maxOf(a.x, b.x, c.x) < min.x ||
            minOf(a.x, b.x, c.x) > max.x ||
            maxOf(a.y, b.y, c.y) < min.y ||
            minOf(a.y, b.y, c.y) > max.y ||
            maxOf(a.z, b.z, c.z) < min.z ||
            minOf(a.z, b.z, c.z) > max.z)
    }
}
