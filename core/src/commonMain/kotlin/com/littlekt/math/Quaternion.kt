package com.littlekt.math

import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.sine
import kotlin.math.*

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
open class Quaternion(open val x: Float, open val y: Float, open val z: Float, open val w: Float) {

    constructor(other: Vec4f) : this(other.x, other.y, other.z, other.w)

    constructor(other: Quaternion) : this(other.x, other.y, other.z, other.w)

    constructor() : this(IDENTITY)

    /**
     * The pole of the gimbal lock, if any. Postiive (+1) for north pole, negative (-1) for south
     * pole, zero (0) when no gimbal lock.
     */
    val gimbalPole: Int
        get() {
            val t = y * x + z * w
            return if (t > 0.499f) 1 else if (t < -0.499f) -1 else 0
        }

    /** The rotation around the z-axis. Requires this quaternion to be normalized. */
    val roll: Angle
        get() {
            val pole = gimbalPole
            return if (pole == 0) {
                atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)).radians
            } else {
                (pole * 2f * atan2(y, w)).radians
            }
        }

    /** The rotation around the x-axis. Requires that this quaternion to be normalized. */
    val pitch: Angle
        get() {
            val pole = gimbalPole
            return if (pole == 0) {
                asin((2f * (w * x - z * y).clamp(-1f, 1f))).radians
            } else {
                (pole * PI * 0.5f).radians
            }
        }

    /** The rotation around the y-axis. Requires that this quaternion tobe normalized. */
    val yaw: Angle
        get() {
            return if (gimbalPole == 0) {
                atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)).radians
            } else {
                Angle.ZERO
            }
        }

    /**
     * Checks vector components for equality using [com.littlekt.math.isFuzzyEqual], that is all
     * components must have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec2f, eps: Float = FUZZY_EQ_F): Boolean =
        isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps)

    fun length(): Float = sqrt(sqrLength())

    fun sqrLength(): Float = x * x + y * y + z * z + w * w

    fun sqrDistance(other: Quaternion): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        val dw = z - other.w
        return dx * dx + dy * dy + dz * dz + dw * dw
    }

    fun conjugate(result: MutableQuaternion = MutableQuaternion()): MutableQuaternion {
        result.x = -x
        result.y = -y
        result.z = -z
        return result
    }

    fun invert(result: MutableQuaternion = MutableQuaternion()): MutableQuaternion {
        val scale = 1f / (x * x + y * y + z * z + w * w)
        result.x = -x * scale
        result.y = -y * scale
        result.z = -z * scale
        result.w *= scale
        return result
    }

    fun norm(result: MutableQuaternion = MutableQuaternion()): MutableQuaternion =
        scale(1f / length(), result)

    fun scale(factor: Float, result: MutableQuaternion = MutableQuaternion()): MutableQuaternion {
        result.x *= factor
        result.y *= factor
        result.z *= factor
        result.w *= factor
        return result
    }

    fun setEuler(
        pitch: Angle,
        yaw: Angle,
        roll: Angle,
        result: MutableQuaternion = MutableQuaternion(),
    ): MutableQuaternion {
        val hr = roll * 0.5f
        val shr = hr.sine
        val chr = hr.cosine

        val hp = pitch * 0.5f
        val shp = hp.sine
        val chp = hp.cosine

        val hy = yaw * 0.5f
        val shy = hy.sine
        val chy = hy.cosine

        val chy_shp = chy * shp
        val shy_chp = shy * chp
        val chy_chp = chy * chp
        val shy_shp = shy * shp

        result.x =
            (chy_shp * chr) +
                (shy_chp *
                    shr) // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) *
        // sin(roll/2)
        result.y =
            (shy_chp * chr) -
                (chy_shp *
                    shr) // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) *
        // sin(roll/2)
        result.z =
            (chy_chp * shr) -
                (shy_shp *
                    chr) // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) *
        // cos(roll/2)
        result.w =
            (chy_chp * chr) +
                (shy_shp *
                    shr) // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) *
        // sin(roll/2)
        return result
    }

    fun subtract(other: Quaternion, result: MutableQuaternion): MutableQuaternion =
        result.set(this).subtract(other)

    fun add(other: Quaternion, result: MutableQuaternion): MutableQuaternion =
        result.set(this).add(other)

    /**
     * Multiplies this vector as a quaternion with another in the form of `this * other`.
     *
     * @param other the quaternion to multiply with
     * @return [result]
     */
    fun mul(other: Quaternion, result: MutableQuaternion): MutableQuaternion {
        val px = w * other.x + x * other.w + y * other.z - z * other.y
        val py = w * other.y + y * other.w + z * other.x - x * other.z
        val pz = w * other.z + z * other.w + x * other.y - y * other.x
        val pw = w * other.w - x * other.x - y * other.y - z * other.z
        result.set(px, py, pz, pw)
        return result
    }

    /**
     * Multiplies this vector as a quaternion with another in the form of `other * this`.
     *
     * @param other the quaternion to multiply with
     * @return [result]
     */
    fun mulLeft(other: Quaternion, result: MutableQuaternion): MutableQuaternion {
        val px = other.w * x + other.x * w + other.y * z - other.z * y
        val py = other.w * y + other.y * w + other.z * x - other.x * z
        val pz = other.w * z + other.z * w + other.x * y - other.y * x
        val pw = other.w * w - other.x * x - other.y * y - other.z * z
        result.set(px, py, pz, pw)
        return result
    }

    fun distance(other: Quaternion): Float = sqrt(sqrDistance(other))

    fun dot(other: Quaternion): Float = x * other.x + y * other.y + z * other.z + w * other.w

    fun lookAt(forward: Vec3f, up: Vec3f, result: MutableQuaternion): MutableQuaternion {
        tempVec3A.set(forward).norm() // forward normalized
        tempVec3B.set(up).cross(tempVec3A).norm() // right normalized
        tempVec3C.set(tempVec3A).cross(tempVec3B) // new up

        val m00 = tempVec3B.x
        val m01 = tempVec3B.y
        val m02 = tempVec3B.z

        val m10 = tempVec3C.x
        val m11 = tempVec3C.y
        val m12 = tempVec3C.z

        val m20 = tempVec3A.x
        val m21 = tempVec3A.y
        val m22 = tempVec3A.z

        val num8 = (m00 + m11) + m22
        temp1.set(0f, 0f, 0f, 1f)
        if (num8 > 0f) {
            var num = sqrt(num8 + 1f)
            temp1.w = num * 0.5f
            num = 0.5f / num
            temp1.x = (m12 - m21) * num
            temp1.y = (m20 - m02) * num
            temp1.z = (m01 - m10) * num
            return result.set(temp1)
        }

        if ((m00 >= m11) && (m00 >= m22)) {
            val num7 = sqrt(((1f + m00) - m11) - m22)
            val num4 = 0.5f / num7
            temp1.x = 0.5f * num7
            temp1.y = (m01 + m10) * num4
            temp1.z = (m02 + m20) * num4
            temp1.w = (m12 - m21) * num4
            return result.set(temp1)
        }

        if (m11 > m22) {
            val num6 = sqrt(((1f + m11) - m00) - m22)
            val num3 = 0.5f / num6
            temp1.x = (m10 + m01) * num3
            temp1.y = 0.5f * num6
            temp1.z = (m21 + m12) * num3
            temp1.w = (m20 - m02) * num3
            return result.set(temp1)
        }

        val num5 = sqrt(((1f + m22) - m00) - m11)
        val num2 = 0.5f / num5
        temp1.x = (m20 + m02) * num2
        temp1.y = (m21 + m12) * num2
        temp1.z = 0.5f * num5
        temp1.w = (m01 - m10) * num2
        return result.set(temp1)
    }

    fun mix(other: Quaternion, weight: Float, result: MutableQuaternion): MutableQuaternion {
        result.x = other.x * weight + x * (1f - weight)
        result.y = other.y * weight + y * (1f - weight)
        result.z = other.z * weight + z * (1f - weight)
        result.w = other.w * weight + w * (1f - weight)
        return result
    }

    /**
     * Transforms the given vector using this quaternion and outputs the [result].
     *
     * @param vec the vector to use in transforming
     * @param result the output of the transformation
     */
    fun transform(vec: Vec3f, result: MutableVec3f): MutableVec3f {
        temp2.set(this)
        temp2.conjugate()
        temp2.mulLeft(temp1.set(vec.x, vec.y, vec.z, 0f).mulLeft(this))

        result.x = temp2.x
        result.y = temp2.y
        result.z = temp2.z
        return result
    }

    fun toMutableQuaternion(): MutableQuaternion = MutableQuaternion(this)

    operator fun times(other: Quaternion) = mul(other, MutableQuaternion())

    operator fun plus(other: Quaternion) = add(other, MutableQuaternion())

    operator fun minus(other: Quaternion) = subtract(other, MutableQuaternion())

    operator fun minus(factor: Float) = scale(factor)

    operator fun component1(): Float = x

    operator fun component2(): Float = y

    operator fun component3(): Float = z

    operator fun component4(): Float = w

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Quaternion

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (w != other.w) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + w.hashCode()
        return result
    }

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
        private val temp1 = MutableQuaternion(0f, 0f, 0f, 0f)
        private val temp2 = MutableQuaternion(0f, 0f, 0f, 0f)

        private val tempVec3A = MutableVec3f()
        private val tempVec3B = MutableVec3f()
        private val tempVec3C = MutableVec3f()
    }
}

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
open class MutableQuaternion(
    override var x: Float,
    override var y: Float,
    override var z: Float,
    override var w: Float,
) : Quaternion(x, y, z, w) {

    constructor(other: Quaternion) : this(other.x, other.y, other.z, other.w)

    constructor() : this(IDENTITY)

    /**
     * Multiplies this vector as a quaternion with another in the form of `this * other`.
     *
     * @param other the quaternion to multiply with
     */
    fun mul(other: Quaternion): MutableQuaternion = mul(other, this)

    /**
     * Multiplies this vector as a quaternion with another in the form of `other * this`.
     *
     * @param other the quaternion to multiply with
     */
    fun mulLeft(other: Quaternion): MutableQuaternion = mulLeft(other, this)

    fun conjugate(): MutableQuaternion = conjugate(this)

    fun invert(): MutableQuaternion = invert(this)

    fun norm(): MutableQuaternion = norm(this)

    fun identity(): MutableQuaternion = set(IDENTITY)

    fun scale(factor: Float): MutableQuaternion = scale(factor, this)

    fun setEuler(pitch: Angle, yaw: Angle, roll: Angle): MutableQuaternion =
        setEuler(pitch, yaw, roll, this)

    fun lookAt(forward: Vec3f, up: Vec3f): MutableQuaternion = lookAt(forward, up, this)

    fun mix(other: Quaternion, weight: Float): MutableQuaternion = mix(other, weight, this)

    fun rotate(angle: Angle, axis: Vec3f): MutableQuaternion {
        var s = axis.sqrLength()
        if (!isFuzzyEqual(s, 1f)) {
            s = 1f / sqrt(s)
        }

        val rad2 = angle.radians * 0.5f
        val factor = sin(rad2)
        val qx = axis.x * factor * s
        val qy = axis.y * factor * s
        val qz = axis.z * factor * s
        val qw = cos(rad2)

        val tx = w * qx + x * qw + y * qz - z * qy
        val ty = w * qy - x * qz + y * qw + z * qx
        val tz = w * qz + x * qy - y * qx + z * qw
        val tw = w * qw - x * qx - y * qy - z * qz
        set(tx, ty, tz, tw)
        return norm()
    }

    fun set(x: Float, y: Float, z: Float, w: Float): MutableQuaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(other: Vec4f): MutableQuaternion {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        return this
    }

    fun set(other: Quaternion): MutableQuaternion {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        return this
    }

    fun add(other: Quaternion): MutableQuaternion {
        x += other.x
        y += other.y
        z += other.z
        w += other.w
        return this
    }

    fun add(other: Vec4f): MutableQuaternion {
        x += other.x
        y += other.y
        z += other.z
        w += other.w
        return this
    }

    fun subtract(other: Quaternion): MutableQuaternion {
        x -= other.x
        y -= other.y
        z -= other.z
        w -= other.w
        return this
    }

    fun subtract(other: Vec4f): MutableQuaternion {
        x -= other.x
        y -= other.y
        z -= other.z
        w -= other.w
        return this
    }

    operator fun set(i: Int, value: Float) {
        when (i) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> error("$i is out of bounds for Quarternion. Only values: `0,1,2,3` are valid!")
        }
    }

    override fun toString(): String = "($x, $y, $z, $w)"
}
