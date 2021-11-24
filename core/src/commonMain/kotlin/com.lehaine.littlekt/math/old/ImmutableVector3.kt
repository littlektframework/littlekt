package com.lehaine.littlekt.math.old

/**
 *  A Vector in which you can only <strong>read</strong> values.
 * The backing Vector values may be updated by another source and will overwrite any changes made to it.
 * @author Colton Daily
 * @date 9/28/2021
 */
class ImmutableVector3(private val delegate: Vector3) {

    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f) : this(Vector3(x, y, z))

    val x: Float
        get() = delegate.x
    val y: Float
        get() = delegate.y
    val z: Float
        get() = delegate.z

    val width: Float
        get() = delegate.x
    val height: Float
        get() = delegate.y
    val deep: Float
        get() = delegate.z

    fun mutable(): Vector3 {
        return delegate.copy()
    }

    fun add(other: ImmutableVector3): Vector3 {
        return add(other.mutable())
    }

    fun add(other: Vector3): Vector3 {
        return mutable().add(other)
    }

    fun toFloat3(): Float3 = Float3(x, y, z)

    fun dist2(other: Vector3) = delegate.dist2(other)
    fun dist2(other: ImmutableVector3) = delegate.dist2(other.delegate)

    operator fun component1(): Float = x
    operator fun component2(): Float = y
    operator fun component3(): Float = z

    override fun toString(): String = delegate.toString()
}