package com.littlekt.math

import com.littlekt.math.spatial.BoundingBox
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A ray with an [origin] and a normalized [direction]. The `intersect` functions return the
 * distance along the ray to the hit, or [NO_HIT] (`-1`) if there is none. Use [pointAt] to get the
 * hit point.
 *
 * @author Colton Daily
 * @date 11/23/2021
 */
class Ray {
    val origin = MutableVec3f()
    val direction = MutableVec3f()

    fun set(other: Ray) {
        origin.set(other.origin)
        direction.set(other.direction)
    }

    fun setFromLookAt(origin: Vec3f, lookAt: Vec3f) {
        this.origin.set(origin)
        direction.set(lookAt).subtract(origin).norm()
    }

    fun distanceToPoint(point: Vec3f): Float = point.distanceToRay(origin, direction)

    fun sqrDistanceToPoint(point: Vec3f): Float = point.sqrDistanceToRay(origin, direction)

    fun sqrDistanceToPoint(x: Float, y: Float, z: Float) =
        sqrDistancePointToRay(x, y, z, origin, direction)

    fun sphereIntersection(center: Vec3f, radius: Float, result: MutableVec3f): Boolean {
        result.set(origin).subtract(center)
        val a = direction * direction
        val b = result * direction * 2f
        val c = result * result - radius * radius
        val discr = b * b - 4 * a * c

        if (discr < 0f) {
            return false
        }

        val numerator = -b - sqrt(discr)
        if (numerator > 0f) {
            val d = numerator / (2f * a)
            result.set(direction).scale(d).add(origin)
            return true
        }

        val numerator2 = -b + sqrt(discr)
        if (numerator2 > 0f) {
            val d = numerator2 / (2f * a)
            result.set(direction).scale(d).add(origin)
            return true
        }

        return false
    }

    /** Sets [result] to the point [distance] along the ray. */
    fun pointAt(distance: Float, result: MutableVec3f): MutableVec3f =
        result.set(direction).scale(distance).add(origin)

    /**
     * @return the distance to where the ray enters the box, `0` if [origin] is inside it, or
     *   [NO_HIT]
     */
    fun intersectBox(box: BoundingBox): Float =
        if (box.isEmpty) NO_HIT
        else intersectBox(box.min.x, box.min.y, box.min.z, box.max.x, box.max.y, box.max.z)

    /**
     * @return the distance to where the ray enters the box, `0` if [origin] is inside it, or
     *   [NO_HIT]
     */
    fun intersectBox(
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
    ): Float {
        var near = 0f
        var far = Float.MAX_VALUE
        for (axis in 0..2) {
            val o: Float
            val d: Float
            val lo: Float
            val hi: Float
            when (axis) {
                0 -> {
                    o = origin.x
                    d = direction.x
                    lo = minX
                    hi = maxX
                }
                1 -> {
                    o = origin.y
                    d = direction.y
                    lo = minY
                    hi = maxY
                }
                else -> {
                    o = origin.z
                    d = direction.z
                    lo = minZ
                    hi = maxZ
                }
            }
            if (abs(d) < EPSILON) {
                if (o < lo || o > hi) return NO_HIT
            } else {
                val inv = 1f / d
                val t0 = (lo - o) * inv
                val t1 = (hi - o) * inv
                near = max(near, min(t0, t1))
                far = min(far, max(t0, t1))
                if (near > far) return NO_HIT
            }
        }
        return near
    }

    /** @return the distance to the closest point where the ray hits the sphere, or [NO_HIT] */
    fun intersectSphere(center: Vec3f, radius: Float): Float {
        val ox = origin.x - center.x
        val oy = origin.y - center.y
        val oz = origin.z - center.z
        val b = ox * direction.x + oy * direction.y + oz * direction.z
        val c = ox * ox + oy * oy + oz * oz - radius * radius
        if (c <= 0f) return 0f
        if (b > 0f) return NO_HIT
        val discr = b * b - c
        if (discr < 0f) return NO_HIT
        return -b - sqrt(discr)
    }

    /**
     * @param normal the normalized plane normal
     * @param point any point on the plane
     * @return the distance to where the ray crosses the plane, or [NO_HIT] if it is parallel or
     *   points away
     */
    fun intersectPlane(normal: Vec3f, point: Vec3f): Float =
        intersectPlane(normal, -(normal.x * point.x + normal.y * point.y + normal.z * point.z))

    /**
     * @param normal the normalized plane normal
     * @param d the plane constant, where `normal · p + d = 0` for each point `p` on the plane
     * @return the distance to where the ray crosses the plane, or [NO_HIT] if it is parallel or
     *   points away
     */
    fun intersectPlane(normal: Vec3f, d: Float): Float {
        val denom = normal.x * direction.x + normal.y * direction.y + normal.z * direction.z
        if (abs(denom) < EPSILON) return NO_HIT
        val t = -(normal.x * origin.x + normal.y * origin.y + normal.z * origin.z + d) / denom
        return if (t >= 0f) t else NO_HIT
    }

    /**
     * @param cullBackFaces `true` to miss triangles facing away from the ray, using counter
     *   clockwise winding as the front
     * @return the distance to where the ray hits the triangle, or [NO_HIT]
     */
    fun intersectTriangle(a: Vec3f, b: Vec3f, c: Vec3f, cullBackFaces: Boolean = false): Float =
        intersectTriangle(a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, cullBackFaces)

    /**
     * @param cullBackFaces `true` to miss triangles facing away from the ray, using counter
     *   clockwise winding as the front
     * @return the distance to where the ray hits the triangle, or [NO_HIT]
     */
    fun intersectTriangle(
        ax: Float,
        ay: Float,
        az: Float,
        bx: Float,
        by: Float,
        bz: Float,
        cx: Float,
        cy: Float,
        cz: Float,
        cullBackFaces: Boolean = false,
    ): Float {
        val e1x = bx - ax
        val e1y = by - ay
        val e1z = bz - az
        val e2x = cx - ax
        val e2y = cy - ay
        val e2z = cz - az
        val px = direction.y * e2z - direction.z * e2y
        val py = direction.z * e2x - direction.x * e2z
        val pz = direction.x * e2y - direction.y * e2x
        val det = e1x * px + e1y * py + e1z * pz
        if (cullBackFaces) {
            if (det < EPSILON) return NO_HIT
        } else if (abs(det) < EPSILON) return NO_HIT
        val inv = 1f / det
        val tx = origin.x - ax
        val ty = origin.y - ay
        val tz = origin.z - az
        val u = (tx * px + ty * py + tz * pz) * inv
        if (u < 0f || u > 1f) return NO_HIT
        val qx = ty * e1z - tz * e1y
        val qy = tz * e1x - tx * e1z
        val qz = tx * e1y - ty * e1x
        val v = (direction.x * qx + direction.y * qy + direction.z * qz) * inv
        if (v < 0f || u + v > 1f) return NO_HIT
        val t = (e2x * qx + e2y * qy + e2z * qz) * inv
        return if (t >= 0f) t else NO_HIT
    }

    /**
     * Sets this to [other] transformed by [matrix], without normalizing [direction], so distances
     * returned by [intersectBox], [intersectPlane] & [intersectTriangle] match distances along
     * [other]. Use the inverse of a global transform to test against local geometry.
     */
    fun setTransformed(other: Ray, matrix: Mat4): Ray {
        matrix.transform(other.origin, 1f, origin)
        matrix.transform(other.direction, 0f, direction)
        return this
    }

    fun transformBy(matrix: Mat4) {
        matrix.transform(origin)
        matrix.transform(direction, 0f).norm()
    }

    override fun toString(): String {
        return "{origin=$origin, direction=$direction}"
    }

    companion object {
        /** Returned by the `intersect` functions when the ray misses. */
        const val NO_HIT = -1f

        private const val EPSILON = 1e-8f
    }
}
