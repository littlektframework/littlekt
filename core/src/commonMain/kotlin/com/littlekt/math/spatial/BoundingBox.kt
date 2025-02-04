package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Ray
import com.littlekt.math.Vec3f
import com.littlekt.math.clamp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Represents an axis-aligned 3D bounding box. This class provides various operations for
 * manipulating and querying the bounding box, such as updating its bounds, checking intersections,
 * and transforming it spatially.
 *
 * @author based off implementation of the Kool engine.
 */
open class BoundingBox() {

    private val mutMin = MutableVec3f()
    private val mutMax = MutableVec3f()
    private val mutSize = MutableVec3f()
    private val mutCenter = MutableVec3f()

    /**
     * Indicates whether the `BoundingBox` is currently considered empty. A bounding box is empty
     * when it has not been initialized or when it has been cleared.
     *
     * This property is automatically managed by operations that modify the bounds of the
     * `BoundingBox`. It cannot be set externally.
     */
    var isEmpty = true
        private set

    /**
     * Represents the minimum coordinate vector of the bounding box.
     *
     * This vector defines the smallest bounds of the bounding box in 3D space. It is automatically
     * updated whenever the bounding box is modified, such as by adding points, expanding, or
     * merging with other bounding boxes.
     */
    val min: Vec3f = mutMin

    /** Represents the maximum corner of the bounding box as a 3D vector. This defines the fur */
    val max: Vec3f = mutMax

    /**
     * Represents the size of the `BoundingBox` as a 3D vector, calculated as the difference between
     * its maximum and minimum bounds. The `size` provides the dimensions of the bounding box along
     * each axis (X, Y, Z).
     *
     * The size is automatically updated whenever the bounds of the bounding box are modified. If
     * the bounding box is empty, the size will be represented as a vector with all components set
     * to zero.
     */
    val size: Vec3f = mutSize

    /**
     * Represents the center point of the bounding box.
     *
     * This property is calculated based on the midpoint between the `min` and `max` values of the
     * bounding box, providing a reference to its geometric center in 3D space. Updates to the
     * bounding box's bounds automatically adjust the `center` value.
     *
     * The `center` can be used for operations like positioning, transformations, or collision
     * detection relative to the center of the bounding box.
     */
    val center: Vec3f = mutCenter

    /**
     * Indicates whether the bounding box is currently in batch update mode.
     *
     * When the value is `true`, updates to the bounding box do not trigger immediate recalculation
     * of size and center. Setting this value to `false` after batch updates automatically triggers
     * a recalculation of these properties.
     */
    var isBatchUpdate = false
        set(value) {
            field = value
            if (!value) {
                updateSizeAndCenter()
            }
        }

    constructor(min: Vec3f, max: Vec3f) : this() {
        set(min, max)
    }

    private fun updateSizeAndCenter() {
        if (!isBatchUpdate) {
            // size = max - min
            mutMax.subtract(mutMin, mutSize)
            // center = min + size * 0.5
            size.scale(0.5f, mutCenter).add(min)
        }
    }

    private fun addPoint(point: Vec3f) {
        if (isEmpty) {
            mutMin.set(point)
            mutMax.set(point)
            isEmpty = false
        } else {
            if (point.x < min.x) {
                mutMin.x = point.x
            }
            if (point.y < min.y) {
                mutMin.y = point.y
            }
            if (point.z < min.z) {
                mutMin.z = point.z
            }
            if (point.x > max.x) {
                mutMax.x = point.x
            }
            if (point.y > max.y) {
                mutMax.y = point.y
            }
            if (point.z > max.z) {
                mutMax.z = point.z
            }
        }
    }

    /**
     * Provides a mechanism to perform multiple updates to the bounding box as a single batch
     * operation. Temporarily sets the `isBatchUpdate` property to `true` during the execution of
     * the provided block, allowing batch updates to be performed without triggering redundant
     * computations or updates. Once the block has been executed, the previous value of
     * `isBatchUpdate` is restored.
     *
     * @param block The block of operations to perform on this `BoundingBox` in a batch update mode.
     */
    inline fun batchUpdate(block: BoundingBox.() -> Unit) {
        val wasBatchUpdate = isBatchUpdate
        isBatchUpdate = true
        block()
        isBatchUpdate = wasBatchUpdate
    }

    /**
     * Checks if the current `BoundingBox` is approximately equal to another `BoundingBox` instance.
     * This uses fuzzy comparison for the minimum and maximum components of each bounding box. It
     * also ensures that the `isEmpty` state of both bounding boxes matches.
     *
     * @param other The `BoundingBox` instance to compare with the current bounding box.
     * @return `true` if the bounding boxes are approximately equal in terms of their components and
     *   state; `false` otherwise.
     */
    fun isFuzzyEqual(other: BoundingBox): Boolean {
        return isEmpty == other.isEmpty &&
            min.isFuzzyEqual(other.min) &&
            max.isFuzzyEqual(other.max)
    }

    /** Clears the bounding box by setting [min] and [max] to zeros. */
    fun clear(): BoundingBox {
        isEmpty = true
        mutMin.set(Vec3f.ZERO)
        mutMax.set(Vec3f.ZERO)
        updateSizeAndCenter()
        return this
    }

    /** Add a point to the bounding box. */
    fun add(point: Vec3f): BoundingBox {
        addPoint(point)
        updateSizeAndCenter()
        return this
    }

    /** Add a list of points to the bounding box. */
    fun add(points: List<Vec3f>): BoundingBox {
        add(points, points.indices)
        return this
    }

    /** Add a point to the bounding box. */
    fun add(points: List<Vec3f>, range: IntRange): BoundingBox {
        for (i in range) {
            addPoint(points[i])
        }
        updateSizeAndCenter()
        return this
    }

    /** Add another bounding box to the current bounding box. */
    fun add(aabb: BoundingBox): BoundingBox {
        if (!aabb.isEmpty) {
            addPoint(aabb.min)
            addPoint(aabb.max)
            updateSizeAndCenter()
        }
        return this
    }

    /** Expand the bounding box by [e] as a factor. */
    fun expand(e: Vec3f): BoundingBox {
        if (isEmpty) {
            throw IllegalStateException("Empty BoundingBox cannot be expanded")
        }
        mutMin -= e
        mutMax += e
        updateSizeAndCenter()
        return this
    }

    /**
     * Expands the bounding box based on the signed components of the given vector. Positive
     * components of the vector expand the maximum bounds, while negative components shrink the
     * minimum bounds.
     *
     * @param e The vector representing the signed expansion values for each axis.
     * @return The expanded BoundingBox instance.
     * @throws IllegalStateException If the BoundingBox is empty and the expansion cannot be
     *   performed.
     */
    fun signedExpand(e: Vec3f): BoundingBox {
        if (isEmpty) {
            throw IllegalStateException("Empty BoundingBox cannot be expanded")
        }
        if (e.x > 0) mutMax.x += e.x else mutMin.x += e.x
        if (e.y > 0) mutMax.y += e.y else mutMin.y += e.y
        if (e.z > 0) mutMax.z += e.z else mutMin.z += e.z
        updateSizeAndCenter()
        return this
    }

    /** Set this bounding box to the values of [other]. */
    fun set(other: BoundingBox): BoundingBox {
        mutMin.set(other.min)
        mutMax.set(other.max)
        mutSize.set(other.size)
        mutCenter.set(other.center)
        isEmpty = other.isEmpty
        return this
    }

    /** Set the [min] and [max] points of the bounding box. */
    fun set(min: Vec3f, max: Vec3f): BoundingBox {
        isEmpty = false
        mutMin.set(min)
        mutMax.set(max)
        updateSizeAndCenter()
        return this
    }

    /** Set the [min] and [max] points of the bounding box. */
    fun set(
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
    ): BoundingBox {
        isEmpty = false
        mutMin.set(minX, minY, minZ)
        mutMax.set(maxX, maxY, maxZ)
        updateSizeAndCenter()
        return this
    }

    /** Translate the bounding box current position by the given [offset]. */
    fun translate(offset: Vec3f) = translate(offset.x, offset.y, offset.z)

    /** Translate the bounding box current position by the given offset values */
    fun translate(x: Float, y: Float, z: Float): BoundingBox {
        if (isEmpty) {
            throw IllegalStateException("Empty BoundingBox cannot be moved")
        }
        mutMin.x += x
        mutMin.y += y
        mutMin.z += z
        mutMax.x += x
        mutMax.y += y
        mutMax.z += z
        mutCenter.x += x
        mutCenter.y += y
        mutCenter.z += z
        return this
    }

    /**
     * Merges the bounds of two bounding boxes ([aabb1] and [aabb2]) into the current bounding box.
     * The current bounding box is updated to encompass the minimum and maximum bounds of the two
     * input bounding boxes.
     *
     * @param aabb1 The first bounding box to merge.
     * @param aabb2 The second bounding box to merge.
     * @return The updated bounding box encompassing the merged bounds of [aabb1] and [aabb2].
     */
    fun setMerged(aabb1: BoundingBox, aabb2: BoundingBox): BoundingBox {
        // manual if is faster than min() and max()
        mutMin.x = min(aabb1.min.x, aabb2.min.x)
        mutMin.y = min(aabb1.min.y, aabb2.min.y)
        mutMin.z = min(aabb1.min.z, aabb2.min.z)

        mutMax.x = max(aabb1.max.x, aabb2.max.x)
        mutMax.y = max(aabb1.max.y, aabb2.max.y)
        mutMax.z = max(aabb1.max.z, aabb2.max.z)

        isEmpty = false
        updateSizeAndCenter()
        return this
    }

    /**
     * Checks whether the given 3D point is contained within the bounds defined by this object.
     *
     * @param point The 3D point to check, represented as a [Vec3f].
     * @return `true` if the point lies within the bounds; `false` otherwise.
     */
    operator fun contains(point: Vec3f): Boolean {
        return point.x >= min.x &&
            point.x <= max.x &&
            point.y >= min.y &&
            point.y <= max.y &&
            point.z >= min.z &&
            point.z <= max.z
    }

    /** Checks if the given point defined by its coordinates (x, */
    fun contains(x: Float, y: Float, z: Float): Boolean {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z
    }

    operator fun contains(aabb: BoundingBox): Boolean {
        return aabb.min in this && aabb.max in this
    }

    /**
     * Checks if this bounding box intersects with the given axis-aligned bounding box (AABB).
     *
     * @param aabb The bounding box to test for intersection with.
     * @return `true` if this bounding box intersects with the given bounding box; `false`
     *   otherwise.
     */
    fun isIntersecting(aabb: BoundingBox): Boolean {
        return min.x <= aabb.max.x &&
            max.x >= aabb.min.x &&
            min.y <= aabb.max.y &&
            max.y >= aabb.min.y &&
            min.z <= aabb.max.z &&
            max.z >= aabb.min.z
    }

    /**
     * Clamps the given point to lie within the bounds of this bounding box. Adjusts the `x`, `y`,
     * and `z` components of the input point such that they remain within the minimum and maximum
     * bounds defined by the bounding box.
     *
     * @param point The mutable 3D vector [MutableVec3f] to be clamped. Its coordinates will be
     *   modified to remain within the bounding box's bounds.
     */
    fun clampToBounds(point: MutableVec3f) {
        point.x = point.x.clamp(min.x, max.x)
        point.y = point.y.clamp(min.y, max.y)
        point.z = point.z.clamp(min.z, max.z)
    }

    /**
     * Computes the distance between the given point and this BoundingBox. It this BoundingBox
     * includes the point, 0 is returned.
     */
    fun pointDistance(pt: Vec3f): Float {
        return sqrt(pointDistanceSqr(pt).toDouble()).toFloat()
    }

    /**
     * Computes the squared distance between the given point and this BoundingBox. It this
     * BoundingBox includes the point, 0 is returned.
     */
    fun pointDistanceSqr(pt: Vec3f): Float {
        if (pt in this) {
            return 0f
        }

        var x = 0.0f
        var y = 0.0f
        var z = 0.0f

        var tmp = pt.x - min.x
        if (tmp < 0) {
            // px < minX
            x = tmp
        } else {
            tmp = max.x - pt.x
            if (tmp < 0) {
                // px > maxX
                x = tmp
            }
        }

        tmp = pt.y - min.y
        if (tmp < 0) {
            // py < minY
            y = tmp
        } else {
            tmp = max.y - pt.y
            if (tmp < 0) {
                // py > maxY
                y = tmp
            }
        }

        tmp = pt.z - min.z
        if (tmp < 0) {
            // pz < minZ
            z = tmp
        } else {
            tmp = max.z - pt.z
            if (tmp < 0) {
                // pz > maxZ
                z = tmp
            }
        }

        return x * x + y * y + z * z
    }

    /**
     * Computes the squared hit distance for the given ray. If the ray does not intersect this
     * BoundingBox Float.MAX_VALUE is returned. If the ray origin is inside this BoundingBox 0 is
     * returned. The method returns the squared distance because it's faster to compute. If the
     * exact distance is needed the square root of the result has to be taken.
     *
     * @param ray The ray to test
     * @return squared distance between origin and the hit point on the BoundingBox surface or
     *   Float.MAX_VALUE if the ray does not intersects the BoundingBox
     */
    fun hitDistanceSqr(ray: Ray): Float {
        var tmin: Float
        var tmax: Float
        val tymin: Float
        val tymax: Float
        val tzmin: Float
        val tzmax: Float

        if (isEmpty) {
            return Float.MAX_VALUE
        }
        if (ray.origin in this) {
            return 0f
        }

        var div = 1.0f / ray.direction.x
        if (div >= 0.0f) {
            tmin = (min.x - ray.origin.x) * div
            tmax = (max.x - ray.origin.x) * div
        } else {
            tmin = (max.x - ray.origin.x) * div
            tmax = (min.x - ray.origin.x) * div
        }

        div = 1.0f / ray.direction.y
        if (div >= 0.0f) {
            tymin = (min.y - ray.origin.y) * div
            tymax = (max.y - ray.origin.y) * div
        } else {
            tymin = (max.y - ray.origin.y) * div
            tymax = (min.y - ray.origin.y) * div
        }

        if (tmin > tymax || tymin > tmax) {
            // no intersection
            return Float.MAX_VALUE
        }
        if (tymin > tmin) {
            tmin = tymin
        }
        if (tymax < tmax) {
            tmax = tymax
        }

        div = 1.0f / ray.direction.z
        if (div >= 0.0f) {
            tzmin = (min.z - ray.origin.z) * div
            tzmax = (max.z - ray.origin.z) * div
        } else {
            tzmin = (max.z - ray.origin.z) * div
            tzmax = (min.z - ray.origin.z) * div
        }

        if (tmin > tzmax || tzmin > tmax) {
            // no intersection
            return Float.MAX_VALUE
        }
        if (tzmin > tmin) {
            tmin = tzmin
        }

        if (tmin > 0) {
            // hit! calculate square distance between ray origin and hit point
            var comp = ray.direction.x * tmin
            var dist = comp * comp
            comp = ray.direction.y * tmin
            dist += comp * comp
            comp = ray.direction.z * tmin
            dist += comp * comp
            return dist / ray.direction.sqrLength()
        } else {
            // no intersection: box is behind ray
            return Float.MAX_VALUE
        }
    }

    override fun toString(): String {
        return if (isEmpty) {
            "[empty]"
        } else {
            "[min=$min, max=$max]"
        }
    }

    /**
     * Checks aabb components for equality (using '==' operator). For better numeric stability
     * consider using [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoundingBox) return false

        if (isEmpty != other.isEmpty) return false
        if (min != other.min) return false
        if (max != other.max) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isEmpty.hashCode()
        result = 31 * result + min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }
}
