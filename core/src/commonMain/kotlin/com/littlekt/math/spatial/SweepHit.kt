package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f

/**
 * Where a moving shape first touches another, filled in by the sweeps of [Collision]. Reuse one
 * per query.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class SweepHit {
    /**
     * How far along the motion the shapes touch, from `0` to `1`. `0` means they already overlap
     * at the start. Move by `delta * time` to stop at the contact.
     */
    var time: Float = 1f
        internal set

    /** The contact point on the surface of the shape that was hit. */
    val point = MutableVec3f()

    /** The surface normal at [point], pointing toward the moving shape. */
    val normal = MutableVec3f()

    /** Sets this to [other]. @return this */
    fun set(other: SweepHit): SweepHit {
        time = other.time
        point.set(other.point)
        normal.set(other.normal)
        return this
    }

    internal fun set(time: Float, point: Vec3f, normal: Vec3f): SweepHit {
        this.time = time
        this.point.set(point)
        this.normal.set(normal)
        return this
    }

    override fun toString(): String = "SweepHit(time=$time, point=$point, normal=$normal)"
}
