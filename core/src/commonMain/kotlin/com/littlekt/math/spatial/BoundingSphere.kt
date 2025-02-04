package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f

/**
 * Represents a bounding sphere defined by a center point and a radius. Used in various spatial
 * computations to construct a minimal enclosing sphere or to reset the sphere's state.
 *
 * @property center The center position of the sphere as a 3D vector.
 * @property radius The radius of the sphere, measured from the center to the sphere's surface.
 * @author Colton Daily
 * @date 2/4/2025
 */
open class BoundingSphere {
    private val _center = MutableVec3f()

    /** The center position of the sphere. */
    val center: Vec3f
        get() = _center

    /** The radius of the bounding sphere. */
    var radius: Float = 0f
        private set

    fun addPoint(point: Vec3f): BoundingSphere {
        val dist = center.distance(point)
        if (dist > radius) {
            radius = dist
        }
        return this
    }

    /** Update the bounding sphere with new position and radius. */
    fun update(center: Vec3f, radius: Float = this.radius): BoundingSphere {
        _center.set(center)
        this.radius = radius
        return this
    }

    /** Sets the sphere center to (0,0,0) and the radius to 0. */
    fun clear() {
        _center.set(0f, 0f, 0f)
        radius = 0f
    }

    override fun toString(): String {
        return "BoundingSphere(_center=$_center, radius=$radius)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BoundingSphere

        if (radius != other.radius) return false
        if (_center != other._center) return false

        return true
    }

    override fun hashCode(): Int {
        var result = radius.hashCode()
        result = 31 * result + _center.hashCode()
        return result
    }
}
