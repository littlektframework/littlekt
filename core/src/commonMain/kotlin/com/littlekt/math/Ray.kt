package com.littlekt.math

import kotlin.math.sqrt

/**
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

    fun transformBy(matrix: Mat4) {
        matrix.transform(origin)
        matrix.transform(direction, 0f).norm()
    }

    override fun toString(): String {
        return "{origin=$origin, direction=$direction}"
    }
}
