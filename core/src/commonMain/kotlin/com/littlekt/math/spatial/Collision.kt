package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Overlap tests and sweeps between spheres, [Capsule]s, axis aligned [BoundingBox]es, and
 * triangles. A sweep moves a shape by a `delta` and finds where it first touches another, filling
 * in a [SweepHit]. Nothing is allocated. Not thread safe.
 *
 * ```
 * if (Collision.sweepCapsuleBox(player, velocity * dt, wall, hit)) {
 *     player.translate(velocity.x * dt * hit.time, ...)
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
object Collision {
    private const val EPSILON = 1e-6f
    private const val BARYCENTRIC_EPSILON = 1e-5f

    private val tmpA = MutableVec3f()
    private val tmpB = MutableVec3f()
    private val tmpC = MutableVec3f()
    private val tmpD = MutableVec3f()
    private val tmpE = MutableVec3f()
    private val tmpF = MutableVec3f()
    private val tmpN = MutableVec3f()
    private val cornerA = MutableVec3f()
    private val cornerB = MutableVec3f()
    private val bestHit = SweepHit()
    private val edgeHit = SweepHit()

    private var segS = 0f
    private var segT = 0f
    private var boxAxis = -1
    private var boxSign = 0f

    /** @return `true` if the two spheres overlap */
    fun overlapSphereSphere(centerA: Vec3f, radiusA: Float, centerB: Vec3f, radiusB: Float): Boolean {
        val r = radiusA + radiusB
        return centerA.sqrDistance(centerB) <= r * r
    }

    /** @return `true` if the sphere overlaps [box] */
    fun overlapSphereBox(center: Vec3f, radius: Float, box: BoundingBox): Boolean {
        if (box.isEmpty) return false
        return boxDistanceSqr(center.x, center.y, center.z, box) <= radius * radius
    }

    /** @return `true` if the sphere overlaps the triangle [a], [b], [c] */
    fun overlapSphereTriangle(center: Vec3f, radius: Float, a: Vec3f, b: Vec3f, c: Vec3f): Boolean =
        closestPointOnTriangle(center, a, b, c, tmpA).sqrDistance(center) <= radius * radius

    /** @return `true` if the sphere overlaps [capsule] */
    fun overlapSphereCapsule(center: Vec3f, radius: Float, capsule: Capsule): Boolean {
        val r = radius + capsule.radius
        return closestPointOnSegment(center, capsule.a, capsule.b, tmpA).sqrDistance(center) <= r * r
    }

    /** @return `true` if the two capsules overlap */
    fun overlapCapsuleCapsule(capsuleA: Capsule, capsuleB: Capsule): Boolean {
        val r = capsuleA.radius + capsuleB.radius
        return segmentDistanceSqr(capsuleA.a, capsuleA.b, capsuleB.a, capsuleB.b) <= r * r
    }

    /** @return `true` if [capsule] overlaps [box] */
    fun overlapCapsuleBox(capsule: Capsule, box: BoundingBox): Boolean {
        if (box.isEmpty) return false
        val a = capsule.a
        val b = capsule.b
        return sphereBoxTime(a.x, a.y, a.z, capsule.radius, b.x - a.x, b.y - a.y, b.z - a.z, box) >= 0f
    }

    /** @return `true` if [capsule] overlaps the triangle [a], [b], [c] */
    fun overlapCapsuleTriangle(capsule: Capsule, a: Vec3f, b: Vec3f, c: Vec3f): Boolean {
        val p = capsule.a
        val q = capsule.b
        return sphereTriangleTime(p, capsule.radius, q.x - p.x, q.y - p.y, q.z - p.z, a, b, c) >= 0f
    }

    /**
     * Moves the sphere at [center] by [delta] and finds where it first touches the sphere at
     * [target].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepSphereSphere(
        center: Vec3f,
        radius: Float,
        delta: Vec3f,
        target: Vec3f,
        targetRadius: Float,
        hit: SweepHit,
    ): Boolean {
        val t =
            raySphereTime(
                center.x, center.y, center.z, delta.x, delta.y, delta.z,
                target.x, target.y, target.z, radius + targetRadius,
            )
        if (t < 0f) return false
        movedPoint(center, delta, t, tmpC)
        normalTo(tmpC, target, delta, tmpN)
        tmpA.set(tmpN).scale(targetRadius).add(target)
        hit.set(t, tmpA, tmpN)
        return true
    }

    /**
     * Moves the sphere at [center] by [delta] and finds where it first touches [box].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepSphereBox(center: Vec3f, radius: Float, delta: Vec3f, box: BoundingBox, hit: SweepHit): Boolean {
        if (box.isEmpty) return false
        val t = sphereBoxTime(center.x, center.y, center.z, radius, delta.x, delta.y, delta.z, box)
        if (t < 0f) return false
        movedPoint(center, delta, t, tmpC)
        boxContact(tmpC, box, delta, hit, t)
        return true
    }

    /**
     * Moves the sphere at [center] by [delta] and finds where it first touches the triangle [a],
     * [b], [c]. Both sides of the triangle are solid.
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepSphereTriangle(
        center: Vec3f,
        radius: Float,
        delta: Vec3f,
        a: Vec3f,
        b: Vec3f,
        c: Vec3f,
        hit: SweepHit,
    ): Boolean {
        val t = sphereTriangleTime(center, radius, delta.x, delta.y, delta.z, a, b, c)
        if (t < 0f) return false
        movedPoint(center, delta, t, tmpC)
        triangleContact(tmpC, a, b, c, delta, hit, t)
        return true
    }

    /**
     * Moves the sphere at [center] by [delta] and finds where it first touches [capsule].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepSphereCapsule(
        center: Vec3f,
        radius: Float,
        delta: Vec3f,
        capsule: Capsule,
        hit: SweepHit,
    ): Boolean {
        val p = capsule.a
        val q = capsule.b
        val t =
            rayCapsuleTime(
                center.x, center.y, center.z, delta.x, delta.y, delta.z,
                p.x, p.y, p.z, q.x, q.y, q.z, radius + capsule.radius,
            )
        if (t < 0f) return false
        movedPoint(center, delta, t, tmpC)
        closestPointOnSegment(tmpC, p, q, tmpD)
        normalTo(tmpC, tmpD, delta, tmpN)
        tmpA.set(tmpN).scale(capsule.radius).add(tmpD)
        hit.set(t, tmpA, tmpN)
        return true
    }

    /**
     * Moves [capsule] by [delta] and finds where it first touches the sphere at [center].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepCapsuleSphere(
        capsule: Capsule,
        delta: Vec3f,
        center: Vec3f,
        radius: Float,
        hit: SweepHit,
    ): Boolean {
        val p = capsule.a
        val q = capsule.b
        val t =
            rayCapsuleTime(
                center.x, center.y, center.z, -delta.x, -delta.y, -delta.z,
                p.x, p.y, p.z, q.x, q.y, q.z, radius + capsule.radius,
            )
        if (t < 0f) return false
        movedPoint(p, delta, t, tmpA)
        movedPoint(q, delta, t, tmpB)
        closestPointOnSegment(center, tmpA, tmpB, tmpD)
        normalTo(tmpD, center, delta, tmpN)
        tmpC.set(tmpN).scale(radius).add(center)
        hit.set(t, tmpC, tmpN)
        return true
    }

    /**
     * Moves [capsule] by [delta] and finds where it first touches [target].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepCapsuleCapsule(capsule: Capsule, delta: Vec3f, target: Capsule, hit: SweepHit): Boolean =
        segmentSweep(capsule.a, capsule.b, capsule.radius, delta, target.a, target.b, target.radius, hit)

    /**
     * Moves [capsule] by [delta] and finds where it first touches [box].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepCapsuleBox(capsule: Capsule, delta: Vec3f, box: BoundingBox, hit: SweepHit): Boolean {
        if (box.isEmpty) return false
        val p = capsule.a
        val q = capsule.b
        val r = capsule.radius
        if (overlapCapsuleBox(capsule, box)) {
            closestPointOnSegment(box.center, p, q, tmpC)
            boxContact(tmpC, box, delta, hit, 0f)
            return true
        }
        bestHit.time = 2f
        if (sweepSphereBox(p, r, delta, box, edgeHit)) bestHit.set(edgeHit)
        if (sweepSphereBox(q, r, delta, box, edgeHit) && edgeHit.time < bestHit.time) bestHit.set(edgeHit)
        for (i in 0 until 8) {
            for (bit in AXIS_BITS) {
                if (i and bit != 0) continue
                corner(box, i, cornerA)
                corner(box, i or bit, cornerB)
                if (segmentSweep(p, q, r, delta, cornerA, cornerB, 0f, edgeHit) && edgeHit.time < bestHit.time) {
                    bestHit.set(edgeHit)
                }
            }
        }
        if (bestHit.time > 1f) return false
        hit.set(bestHit)
        return true
    }

    /**
     * Moves [capsule] by [delta] and finds where it first touches the triangle [a], [b], [c]. Both
     * sides of the triangle are solid.
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepCapsuleTriangle(
        capsule: Capsule,
        delta: Vec3f,
        a: Vec3f,
        b: Vec3f,
        c: Vec3f,
        hit: SweepHit,
    ): Boolean {
        val p = capsule.a
        val q = capsule.b
        val r = capsule.radius
        bestHit.time = 2f
        if (overlapCapsuleTriangle(capsule, a, b, c)) {
            closestPointOnTriangle(p, a, b, c, tmpD)
            closestPointOnTriangle(q, a, b, c, tmpE)
            val origin = if (p.sqrDistance(tmpD) <= q.sqrDistance(tmpE)) p else q
            triangleContact(origin, a, b, c, delta, hit, 0f)
            return true
        }
        if (sweepSphereTriangle(p, r, delta, a, b, c, edgeHit)) bestHit.set(edgeHit)
        if (sweepSphereTriangle(q, r, delta, a, b, c, edgeHit) && edgeHit.time < bestHit.time) bestHit.set(edgeHit)
        if (segmentSweep(p, q, r, delta, a, b, 0f, edgeHit) && edgeHit.time < bestHit.time) bestHit.set(edgeHit)
        if (segmentSweep(p, q, r, delta, b, c, 0f, edgeHit) && edgeHit.time < bestHit.time) bestHit.set(edgeHit)
        if (segmentSweep(p, q, r, delta, c, a, 0f, edgeHit) && edgeHit.time < bestHit.time) bestHit.set(edgeHit)
        if (bestHit.time > 1f) return false
        hit.set(bestHit)
        return true
    }

    /**
     * Moves [box] by [delta] and finds where it first touches [target].
     *
     * @return `true` if they touch, with the contact in [hit]
     */
    fun sweepBoxBox(box: BoundingBox, delta: Vec3f, target: BoundingBox, hit: SweepHit): Boolean {
        if (box.isEmpty || target.isEmpty) return false
        val hx = box.size.x * 0.5f
        val hy = box.size.y * 0.5f
        val hz = box.size.z * 0.5f
        val center = box.center
        val t =
            rayBoxTime(
                center.x, center.y, center.z, delta.x, delta.y, delta.z,
                target.min.x - hx, target.min.y - hy, target.min.z - hz,
                target.max.x + hx, target.max.y + hy, target.max.z + hz,
            )
        if (t < 0f) return false
        tmpN.set(0f, 0f, 0f)
        if (t > 0f && boxAxis >= 0) {
            tmpN[boxAxis] = boxSign
        } else {
            movedPoint(center, delta, t, tmpC)
            penetrationNormal(tmpC, target, hx, hy, hz, tmpN)
        }
        movedPoint(center, delta, t, tmpC)
        tmpA.set(
            tmpC.x.coerceIn(target.min.x, target.max.x),
            tmpC.y.coerceIn(target.min.y, target.max.y),
            tmpC.z.coerceIn(target.min.z, target.max.z),
        )
        hit.set(t, tmpA, tmpN)
        return true
    }

    /** Sets [result] to the box around a sphere as it moves by [delta]. @return [result] */
    fun sweepBounds(center: Vec3f, radius: Float, delta: Vec3f, result: BoundingBox): BoundingBox =
        result.set(
            center.x - radius + min(0f, delta.x),
            center.y - radius + min(0f, delta.y),
            center.z - radius + min(0f, delta.z),
            center.x + radius + max(0f, delta.x),
            center.y + radius + max(0f, delta.y),
            center.z + radius + max(0f, delta.z),
        )

    /** Sets [result] to the point on the segment [a], [b] closest to [point]. @return [result] */
    fun closestPointOnSegment(point: Vec3f, a: Vec3f, b: Vec3f, result: MutableVec3f): MutableVec3f {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val abz = b.z - a.z
        val len = abx * abx + aby * aby + abz * abz
        val t =
            if (len <= EPSILON) 0f
            else (((point.x - a.x) * abx + (point.y - a.y) * aby + (point.z - a.z) * abz) / len).coerceIn(0f, 1f)
        return result.set(a.x + abx * t, a.y + aby * t, a.z + abz * t)
    }

    /** Sets [result] to the point on the triangle [a], [b], [c] closest to [p]. @return [result] */
    fun closestPointOnTriangle(p: Vec3f, a: Vec3f, b: Vec3f, c: Vec3f, result: MutableVec3f): MutableVec3f {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val abz = b.z - a.z
        val acx = c.x - a.x
        val acy = c.y - a.y
        val acz = c.z - a.z
        val apx = p.x - a.x
        val apy = p.y - a.y
        val apz = p.z - a.z
        val d1 = abx * apx + aby * apy + abz * apz
        val d2 = acx * apx + acy * apy + acz * apz
        if (d1 <= 0f && d2 <= 0f) return result.set(a)
        val bpx = p.x - b.x
        val bpy = p.y - b.y
        val bpz = p.z - b.z
        val d3 = abx * bpx + aby * bpy + abz * bpz
        val d4 = acx * bpx + acy * bpy + acz * bpz
        if (d3 >= 0f && d4 <= d3) return result.set(b)
        val vc = d1 * d4 - d3 * d2
        if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
            val v = d1 / (d1 - d3)
            return result.set(a.x + abx * v, a.y + aby * v, a.z + abz * v)
        }
        val cpx = p.x - c.x
        val cpy = p.y - c.y
        val cpz = p.z - c.z
        val d5 = abx * cpx + aby * cpy + abz * cpz
        val d6 = acx * cpx + acy * cpy + acz * cpz
        if (d6 >= 0f && d5 <= d6) return result.set(c)
        val vb = d5 * d2 - d1 * d6
        if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
            val w = d2 / (d2 - d6)
            return result.set(a.x + acx * w, a.y + acy * w, a.z + acz * w)
        }
        val va = d3 * d6 - d5 * d4
        if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {
            val w = (d4 - d3) / ((d4 - d3) + (d5 - d6))
            return result.set(b.x + (c.x - b.x) * w, b.y + (c.y - b.y) * w, b.z + (c.z - b.z) * w)
        }
        val denom = 1f / (va + vb + vc)
        val v = vb * denom
        val w = vc * denom
        return result.set(a.x + abx * v + acx * w, a.y + aby * v + acy * w, a.z + abz * v + acz * w)
    }

    /**
     * @return the squared distance between the segments [p1], [q1] and [p2], [q2]
     */
    fun segmentDistanceSqr(p1: Vec3f, q1: Vec3f, p2: Vec3f, q2: Vec3f): Float {
        val d1x = q1.x - p1.x
        val d1y = q1.y - p1.y
        val d1z = q1.z - p1.z
        val d2x = q2.x - p2.x
        val d2y = q2.y - p2.y
        val d2z = q2.z - p2.z
        val rx = p1.x - p2.x
        val ry = p1.y - p2.y
        val rz = p1.z - p2.z
        val a = d1x * d1x + d1y * d1y + d1z * d1z
        val e = d2x * d2x + d2y * d2y + d2z * d2z
        val f = d2x * rx + d2y * ry + d2z * rz
        var s: Float
        var t: Float
        if (a <= EPSILON && e <= EPSILON) {
            s = 0f
            t = 0f
        } else if (a <= EPSILON) {
            s = 0f
            t = (f / e).coerceIn(0f, 1f)
        } else {
            val c = d1x * rx + d1y * ry + d1z * rz
            if (e <= EPSILON) {
                t = 0f
                s = (-c / a).coerceIn(0f, 1f)
            } else {
                val b = d1x * d2x + d1y * d2y + d1z * d2z
                val denom = a * e - b * b
                s = if (denom > EPSILON) ((b * f - c * e) / denom).coerceIn(0f, 1f) else 0f
                t = (b * s + f) / e
                if (t < 0f) {
                    t = 0f
                    s = (-c / a).coerceIn(0f, 1f)
                } else if (t > 1f) {
                    t = 1f
                    s = ((b - c) / a).coerceIn(0f, 1f)
                }
            }
        }
        segS = s
        segT = t
        val dx = rx + d1x * s - d2x * t
        val dy = ry + d1y * s - d2y * t
        val dz = rz + d1z * s - d2z * t
        return dx * dx + dy * dy + dz * dz
    }

    private fun segmentSweep(
        a0: Vec3f,
        a1: Vec3f,
        radiusA: Float,
        delta: Vec3f,
        b0: Vec3f,
        b1: Vec3f,
        radiusB: Float,
        hit: SweepHit,
    ): Boolean {
        val r = radiusA + radiusB
        var best = 2f
        if (segmentDistanceSqr(a0, a1, b0, b1) <= r * r) {
            best = 0f
        } else {
            val cx = b0.x - a0.x
            val cy = b0.y - a0.y
            val cz = b0.z - a0.z
            val e1x = b1.x - b0.x
            val e1y = b1.y - b0.y
            val e1z = b1.z - b0.z
            val e2x = a0.x - a1.x
            val e2y = a0.y - a1.y
            val e2z = a0.z - a1.z
            val dx = delta.x
            val dy = delta.y
            val dz = delta.z
            best = minTime(best, rayCapsuleTime(0f, 0f, 0f, dx, dy, dz, cx, cy, cz, cx + e1x, cy + e1y, cz + e1z, r))
            best =
                minTime(
                    best,
                    rayCapsuleTime(
                        0f, 0f, 0f, dx, dy, dz,
                        cx + e2x, cy + e2y, cz + e2z, cx + e1x + e2x, cy + e1y + e2y, cz + e1z + e2z, r,
                    ),
                )
            best = minTime(best, rayCapsuleTime(0f, 0f, 0f, dx, dy, dz, cx, cy, cz, cx + e2x, cy + e2y, cz + e2z, r))
            best =
                minTime(
                    best,
                    rayCapsuleTime(
                        0f, 0f, 0f, dx, dy, dz,
                        cx + e1x, cy + e1y, cz + e1z, cx + e1x + e2x, cy + e1y + e2y, cz + e1z + e2z, r,
                    ),
                )
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val nl = sqrt(nx * nx + ny * ny + nz * nz)
            if (nl > EPSILON) {
                nx /= nl
                ny /= nl
                nz /= nl
                val dn = dx * nx + dy * ny + dz * nz
                if (abs(dn) > EPSILON) {
                    val e11 = e1x * e1x + e1y * e1y + e1z * e1z
                    val e12 = e1x * e2x + e1y * e2y + e1z * e2z
                    val e22 = e2x * e2x + e2y * e2y + e2z * e2z
                    val det = e11 * e22 - e12 * e12
                    if (abs(det) > EPSILON) {
                        for (side in SIDES) {
                            val ox = cx + nx * r * side
                            val oy = cy + ny * r * side
                            val oz = cz + nz * r * side
                            val t = (ox * nx + oy * ny + oz * nz) / dn
                            if (t < 0f || t > 1f || t >= best) continue
                            val hx = dx * t - ox
                            val hy = dy * t - oy
                            val hz = dz * t - oz
                            val h1 = hx * e1x + hy * e1y + hz * e1z
                            val h2 = hx * e2x + hy * e2y + hz * e2z
                            val u = (h1 * e22 - h2 * e12) / det
                            val v = (h2 * e11 - h1 * e12) / det
                            if (u in 0f..1f && v in 0f..1f) best = t
                        }
                    }
                }
            }
        }
        if (best > 1f) return false
        movedPoint(a0, delta, best, tmpA)
        movedPoint(a1, delta, best, tmpB)
        segmentDistanceSqr(tmpA, tmpB, b0, b1)
        tmpC.set(tmpA).scale(1f - segS).add(tmpD.set(tmpB).scale(segS))
        tmpD.set(b0).scale(1f - segT).add(tmpE.set(b1).scale(segT))
        normalTo(tmpC, tmpD, delta, tmpN)
        tmpE.set(tmpN).scale(radiusB).add(tmpD)
        hit.set(best, tmpE, tmpN)
        return true
    }

    private fun sphereBoxTime(
        cx: Float,
        cy: Float,
        cz: Float,
        r: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        box: BoundingBox,
    ): Float {
        val min = box.min
        val max = box.max
        if (boxDistanceSqr(cx, cy, cz, box) <= r * r) return 0f
        val t =
            rayBoxTime(cx, cy, cz, dx, dy, dz, min.x - r, min.y - r, min.z - r, max.x + r, max.y + r, max.z + r)
        if (t < 0f) return -1f
        val px = cx + dx * t
        val py = cy + dy * t
        val pz = cz + dz * t
        var u = 0
        var v = 0
        if (px < min.x) u = u or 1
        if (px > max.x) v = v or 1
        if (py < min.y) u = u or 2
        if (py > max.y) v = v or 2
        if (pz < min.z) u = u or 4
        if (pz > max.z) v = v or 4
        val m = u or v
        if (m and (m - 1) == 0) return t
        if (m == 7) {
            var best = 2f
            for (bit in AXIS_BITS) {
                best = minTime(best, boxEdgeTime(cx, cy, cz, dx, dy, dz, box, v, v xor bit, r))
            }
            return if (best > 1f) -1f else best
        }
        return boxEdgeTime(cx, cy, cz, dx, dy, dz, box, u xor 7, v, r)
    }

    private fun boxEdgeTime(
        cx: Float,
        cy: Float,
        cz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        box: BoundingBox,
        from: Int,
        to: Int,
        r: Float,
    ): Float {
        corner(box, from, tmpE)
        corner(box, to, tmpF)
        return rayCapsuleTime(cx, cy, cz, dx, dy, dz, tmpE.x, tmpE.y, tmpE.z, tmpF.x, tmpF.y, tmpF.z, r)
    }

    private fun sphereTriangleTime(
        center: Vec3f,
        r: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        a: Vec3f,
        b: Vec3f,
        c: Vec3f,
    ): Float {
        if (overlapSphereTriangle(center, r, a, b, c)) return 0f
        var best = 2f
        val abx = b.x - a.x
        val aby = b.y - a.y
        val abz = b.z - a.z
        val acx = c.x - a.x
        val acy = c.y - a.y
        val acz = c.z - a.z
        var nx = aby * acz - abz * acy
        var ny = abz * acx - abx * acz
        var nz = abx * acy - aby * acx
        val nl = sqrt(nx * nx + ny * ny + nz * nz)
        if (nl > EPSILON) {
            nx /= nl
            ny /= nl
            nz /= nl
            var dist = (center.x - a.x) * nx + (center.y - a.y) * ny + (center.z - a.z) * nz
            if (dist < 0f) {
                nx = -nx
                ny = -ny
                nz = -nz
                dist = -dist
            }
            val dn = dx * nx + dy * ny + dz * nz
            if (dn < -EPSILON) {
                val t = (dist - r) / -dn
                if (t in 0f..1f) {
                    val px = center.x + dx * t - nx * r - a.x
                    val py = center.y + dy * t - ny * r - a.y
                    val pz = center.z + dz * t - nz * r - a.z
                    val d00 = abx * abx + aby * aby + abz * abz
                    val d01 = abx * acx + aby * acy + abz * acz
                    val d11 = acx * acx + acy * acy + acz * acz
                    val d20 = px * abx + py * aby + pz * abz
                    val d21 = px * acx + py * acy + pz * acz
                    val det = d00 * d11 - d01 * d01
                    val v = (d11 * d20 - d01 * d21) / det
                    val w = (d00 * d21 - d01 * d20) / det
                    if (v >= -BARYCENTRIC_EPSILON && w >= -BARYCENTRIC_EPSILON && v + w <= 1f + BARYCENTRIC_EPSILON) {
                        best = t
                    }
                }
            }
        }
        if (best > 1f) {
            best = minTime(best, rayCapsuleTime(center.x, center.y, center.z, dx, dy, dz, a.x, a.y, a.z, b.x, b.y, b.z, r))
            best = minTime(best, rayCapsuleTime(center.x, center.y, center.z, dx, dy, dz, b.x, b.y, b.z, c.x, c.y, c.z, r))
            best = minTime(best, rayCapsuleTime(center.x, center.y, center.z, dx, dy, dz, c.x, c.y, c.z, a.x, a.y, a.z, r))
        }
        return if (best > 1f) -1f else best
    }

    private fun raySphereTime(
        ox: Float,
        oy: Float,
        oz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        cx: Float,
        cy: Float,
        cz: Float,
        r: Float,
    ): Float {
        val mx = ox - cx
        val my = oy - cy
        val mz = oz - cz
        val c = mx * mx + my * my + mz * mz - r * r
        if (c <= 0f) return 0f
        val b = mx * dx + my * dy + mz * dz
        if (b >= 0f) return -1f
        val a = dx * dx + dy * dy + dz * dz
        if (a <= EPSILON) return -1f
        val disc = b * b - a * c
        if (disc < 0f) return -1f
        val t = (-b - sqrt(disc)) / a
        return if (t in 0f..1f) t else -1f
    }

    private fun rayCapsuleTime(
        ox: Float,
        oy: Float,
        oz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        px: Float,
        py: Float,
        pz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        r: Float,
    ): Float {
        val axx = qx - px
        val axy = qy - py
        val axz = qz - pz
        val mx = ox - px
        val my = oy - py
        val mz = oz - pz
        val dd = axx * axx + axy * axy + axz * axz
        val md = mx * axx + my * axy + mz * axz
        val s0 = if (dd <= EPSILON) 0f else (md / dd).coerceIn(0f, 1f)
        val cx = mx - axx * s0
        val cy = my - axy * s0
        val cz = mz - axz * s0
        if (cx * cx + cy * cy + cz * cz <= r * r) return 0f
        var best = 2f
        if (dd > EPSILON) {
            val nd = dx * axx + dy * axy + dz * axz
            val nn = dx * dx + dy * dy + dz * dz
            val mn = mx * dx + my * dy + mz * dz
            val a = dd * nn - nd * nd
            val k = mx * mx + my * my + mz * mz - r * r
            val c = dd * k - md * md
            val b = dd * mn - nd * md
            if (abs(a) > EPSILON) {
                val disc = b * b - a * c
                if (disc >= 0f) {
                    val t = (-b - sqrt(disc)) / a
                    if (t in 0f..1f) {
                        val s = md + t * nd
                        if (s in 0f..dd) best = t
                    }
                }
            }
        }
        best = minTime(best, raySphereTime(ox, oy, oz, dx, dy, dz, px, py, pz, r))
        best = minTime(best, raySphereTime(ox, oy, oz, dx, dy, dz, qx, qy, qz, r))
        return if (best > 1f) -1f else best
    }

    private fun rayBoxTime(
        ox: Float,
        oy: Float,
        oz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
    ): Float {
        var tMin = 0f
        var tMax = 1f
        boxAxis = -1
        boxSign = 0f
        for (axis in 0 until 3) {
            val o = if (axis == 0) ox else if (axis == 1) oy else oz
            val d = if (axis == 0) dx else if (axis == 1) dy else dz
            val lo = if (axis == 0) minX else if (axis == 1) minY else minZ
            val hi = if (axis == 0) maxX else if (axis == 1) maxY else maxZ
            if (abs(d) < EPSILON) {
                if (o < lo || o > hi) return -1f
            } else {
                val inv = 1f / d
                var t1 = (lo - o) * inv
                var t2 = (hi - o) * inv
                var sign = -1f
                if (t1 > t2) {
                    val tmp = t1
                    t1 = t2
                    t2 = tmp
                    sign = 1f
                }
                if (t1 > tMin) {
                    tMin = t1
                    boxAxis = axis
                    boxSign = sign
                }
                if (t2 < tMax) tMax = t2
                if (tMin > tMax) return -1f
            }
        }
        return tMin
    }

    private fun boxContact(center: Vec3f, box: BoundingBox, delta: Vec3f, hit: SweepHit, t: Float) {
        tmpA.set(
            center.x.coerceIn(box.min.x, box.max.x),
            center.y.coerceIn(box.min.y, box.max.y),
            center.z.coerceIn(box.min.z, box.max.z),
        )
        tmpN.set(center).subtract(tmpA)
        if (tmpN.sqrLength() > EPSILON * EPSILON) {
            tmpN.norm()
        } else {
            penetrationNormal(center, box, 0f, 0f, 0f, tmpN)
        }
        hit.set(t, tmpA, tmpN)
    }

    private fun triangleContact(
        center: Vec3f,
        a: Vec3f,
        b: Vec3f,
        c: Vec3f,
        delta: Vec3f,
        hit: SweepHit,
        t: Float,
    ) {
        closestPointOnTriangle(center, a, b, c, tmpA)
        tmpN.set(center).subtract(tmpA)
        if (tmpN.sqrLength() > EPSILON * EPSILON) {
            tmpN.norm()
        } else {
            tmpB.set(b).subtract(a)
            tmpD.set(c).subtract(a)
            tmpB.cross(tmpD, tmpN).norm()
            if (tmpN.dot(delta) > 0f) tmpN.scale(-1f)
        }
        hit.set(t, tmpA, tmpN)
    }

    private fun penetrationNormal(
        center: Vec3f,
        box: BoundingBox,
        hx: Float,
        hy: Float,
        hz: Float,
        result: MutableVec3f,
    ) {
        var best = Float.MAX_VALUE
        result.set(0f, 1f, 0f)
        for (axis in 0 until 3) {
            val c = center[axis]
            val h = if (axis == 0) hx else if (axis == 1) hy else hz
            val toMin = c + h - box.min[axis]
            val toMax = box.max[axis] - (c - h)
            if (toMin < best) {
                best = toMin
                result.set(0f, 0f, 0f)
                result[axis] = -1f
            }
            if (toMax < best) {
                best = toMax
                result.set(0f, 0f, 0f)
                result[axis] = 1f
            }
        }
    }

    private fun boxDistanceSqr(x: Float, y: Float, z: Float, box: BoundingBox): Float {
        val dx = max(max(box.min.x - x, 0f), x - box.max.x)
        val dy = max(max(box.min.y - y, 0f), y - box.max.y)
        val dz = max(max(box.min.z - z, 0f), z - box.max.z)
        return dx * dx + dy * dy + dz * dz
    }

    private fun corner(box: BoundingBox, bits: Int, result: MutableVec3f): MutableVec3f =
        result.set(
            if (bits and 1 != 0) box.max.x else box.min.x,
            if (bits and 2 != 0) box.max.y else box.min.y,
            if (bits and 4 != 0) box.max.z else box.min.z,
        )

    private fun movedPoint(point: Vec3f, delta: Vec3f, t: Float, result: MutableVec3f): MutableVec3f =
        result.set(point.x + delta.x * t, point.y + delta.y * t, point.z + delta.z * t)

    private fun normalTo(from: Vec3f, to: Vec3f, delta: Vec3f, result: MutableVec3f): MutableVec3f {
        result.set(from).subtract(to)
        if (result.sqrLength() > EPSILON * EPSILON) return result.norm()
        result.set(delta).scale(-1f)
        if (result.sqrLength() > EPSILON * EPSILON) return result.norm()
        return result.set(0f, 1f, 0f)
    }

    private fun minTime(best: Float, t: Float): Float = if (t >= 0f && t < best) t else best

    private val AXIS_BITS = intArrayOf(1, 2, 4)
    private val SIDES = floatArrayOf(1f, -1f)
}
