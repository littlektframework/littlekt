package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import kotlin.math.max
import kotlin.math.min

/**
 * A capsule: every point within [radius] of the segment from [a] to [b]. A common shape for
 * characters, since it slides over edges smoothly. Reuse one and move it with [set] or
 * [translate].
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class Capsule(a: Vec3f = Vec3f.ZERO, b: Vec3f = Vec3f.ZERO, radius: Float = 0.5f) {
    /** One end of the capsule's inner segment. */
    val a = MutableVec3f(a)

    /** The other end of the capsule's inner segment. */
    val b = MutableVec3f(b)

    /** The distance from the inner segment to the surface. */
    var radius: Float = radius

    /** Sets both ends of the inner segment and the [radius]. */
    fun set(a: Vec3f, b: Vec3f, radius: Float = this.radius): Capsule {
        this.a.set(a)
        this.b.set(b)
        this.radius = radius
        return this
    }

    /**
     * Sets this to an upright capsule standing on [x], [y], [z] that is [height] tall, including
     * its rounded ends, along the y axis.
     */
    fun setUpright(x: Float, y: Float, z: Float, height: Float, radius: Float = this.radius): Capsule {
        val r = min(radius, height * 0.5f)
        a.set(x, y + r, z)
        b.set(x, y + max(r, height - r), z)
        this.radius = r
        return this
    }

    /** Moves the capsule by [x], [y], [z]. */
    fun translate(x: Float, y: Float, z: Float): Capsule {
        a.add(x, y, z)
        b.add(x, y, z)
        return this
    }

    /** Moves the capsule by [offset]. */
    fun translate(offset: Vec3f): Capsule = translate(offset.x, offset.y, offset.z)

    /** Sets [result] to the box around this capsule. @return [result] */
    fun bounds(result: BoundingBox): BoundingBox =
        result.set(
            min(a.x, b.x) - radius,
            min(a.y, b.y) - radius,
            min(a.z, b.z) - radius,
            max(a.x, b.x) + radius,
            max(a.y, b.y) + radius,
            max(a.z, b.z) + radius,
        )

    /** Sets [result] to the box around this capsule as it moves by [delta]. @return [result] */
    fun sweepBounds(delta: Vec3f, result: BoundingBox): BoundingBox {
        bounds(result)
        return result.set(
            result.min.x + min(0f, delta.x),
            result.min.y + min(0f, delta.y),
            result.min.z + min(0f, delta.z),
            result.max.x + max(0f, delta.x),
            result.max.y + max(0f, delta.y),
            result.max.z + max(0f, delta.z),
        )
    }

    override fun toString(): String = "Capsule(a=$a, b=$b, radius=$radius)"
}
