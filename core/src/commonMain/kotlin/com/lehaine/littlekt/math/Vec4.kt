package com.lehaine.littlekt.math

import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.math.geom.sine
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Vec4f internal constructor(x: Float, y: Float, z: Float, w: Float, size: Int) : Vec3f(x, y, z, size) {
    open val w get() = this[3]

    /**
     * The pole of the gimbal lock, if any.
     * Postiive (+1) for north pole, negative (-1) for south pole, zero (0) who no gimbal lock.
     */
    val gimbalPole: Int
        get() {
            val t = y * x + z * w
            return if (t > 0.499f) 1 else if (t < -0.499f) -1 else 0
        }

    /**
     * The rotation around the z-axis. Requires this vector / quaternion to be normalized.
     */
    val roll: Angle
        get() {
            val pole = gimbalPole
            return if (pole == 0) {
                atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)).radians
            } else {
                (pole * 2f * atan2(y, w)).radians
            }
        }

    /**
     * The rotation around the x-axis. Requires that this vector / quaternion to be normalized.
     */
    val pitch: Angle
        get() {
            val pole = gimbalPole
            return if (pole == 0) {
                asin((2f * (w * x - z * y).clamp(-1f, 1f))).radians
            } else {
                (pole * PI * 0.5f).radians
            }
        }

    /**
     * The rotation around the y-axis. Requires that this vector / quaternion tobe normalized.
     */
    val yaw: Angle
        get() {
            return if (gimbalPole == 0) {
                atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)).radians
            } else {
                Angle.ZERO
            }
        }

    constructor(x: Float, y: Float, z: Float, w: Float) : this(x, y, z, w, 4)
    constructor(f: Float) : this(f, f, f, f)
    constructor(xyz: Vec3f, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(v: Vec4f) : this(v.x, v.y, v.z, v.w)

    init {
        fields[0] = x
        fields[1] = y
        fields[2] = z
        fields[3] = w
    }

    fun add(other: Vec4f, result: MutableVec4f): MutableVec4f = result.set(this).add(other)

    fun distance(other: Vec4f): Float = sqrt(sqrDistance(other))

    fun dot(other: Vec4f): Float = x * other.x + y * other.y + z * other.z + w * other.w

    /**
     * Checks vector components for equality using [com.lehaine.littlekt.math.isFuzzyEqual], that is all components must
     * have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec4f, eps: Float = FUZZY_EQ_F): Boolean =
        isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps) && isFuzzyEqual(z, other.z, eps) && isFuzzyEqual(
            w,
            other.w,
            eps
        )

    /**
     * Transforms the given vector and outputs the [result].
     * @param vec the vector to use in transforming
     * @param result the output of the transformation
     */
    fun transform(vec: Vec3f, result: MutableVec3f): MutableVec3f {
        temp2.set(this)
        temp2.conjugate()
        temp2.mul(temp1.set(vec.x, vec.y, vec.z, 0f).mul(this))

        result.x = temp2.x
        result.y = temp2.y
        result.z = temp2.z
        return result
    }

    fun mix(other: Vec4f, weight: Float, result: MutableVec4f): MutableVec4f {
        result.x = other.x * weight + x * (1f - weight)
        result.y = other.y * weight + y * (1f - weight)
        result.z = other.z * weight + z * (1f - weight)
        result.w = other.w * weight + w * (1f - weight)
        return result
    }

    fun mul(other: Vec4f, result: MutableVec4f): MutableVec4f = result.set(this).mul(other)

    fun norm(result: MutableVec4f): MutableVec4f = result.set(this).norm()

    fun quatMul(otherQuat: Vec4f, result: MutableVec4f): MutableVec4f {
        result.x = w * otherQuat.x + x * otherQuat.w + y * otherQuat.z - z * otherQuat.y
        result.y = w * otherQuat.y + y * otherQuat.w + z * otherQuat.x - x * otherQuat.z
        result.z = w * otherQuat.z + z * otherQuat.w + x * otherQuat.y - y * otherQuat.x
        result.w = w * otherQuat.w - x * otherQuat.x - y * otherQuat.y - z * otherQuat.z
        return result
    }

    fun scale(factor: Float, result: MutableVec4f): MutableVec4f = result.set(this).scale(factor)

    fun sqrDistance(other: Vec4f): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        val dw = z - other.w
        return dx * dx + dy * dy + dz * dz + dw * dw
    }

    override fun sqrLength(): Float = x * x + y * y + z * z + w * w

    fun subtract(other: Vec4f, result: MutableVec4f): MutableVec4f = result.set(this).subtract(other)

    fun getXyz(result: MutableVec3f): MutableVec3f {
        result.x = x
        result.y = y
        result.z = z
        return result
    }

    operator fun times(other: Vec4f): Float = dot(other)

    fun toVec4(): Vec4f = Vec4f(x, y, z, w)

    fun toMutableVec4(): MutableVec4f = toMutableVec4(MutableVec4f())

    fun toMutableVec4(result: MutableVec4f): MutableVec4f = result.set(x, y, z, w)

    override fun toString(): String = "($x, $y, $z, $w)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability consider using
     * [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec4f) return false

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
        val ZERO = Vec4f(0f)
        val X_AXIS = Vec4f(1f, 0f, 0f, 0f)
        val Y_AXIS = Vec4f(0f, 1f, 0f, 0f)
        val Z_AXIS = Vec4f(0f, 0f, 1f, 0f)
        val W_AXIS = Vec4f(0f, 0f, 0f, 1f)
        val NEG_X_AXIS = Vec4f(-1f, 0f, 0f, 0f)
        val NEG_Y_AXIS = Vec4f(0f, -1f, 0f, 0f)
        val NEG_Z_AXIS = Vec4f(0f, 0f, -1f, 0f)
        val NEG_W_AXIS = Vec4f(0f, 0f, 0f, -1f)

        private val temp1 = MutableVec4f()
        private val temp2 = MutableVec4f()
    }
}

open class MutableVec4f(x: Float, y: Float, z: Float, w: Float) : Vec4f(x, y, z, w) {

    override var x
        get() = this[0]
        set(value) {
            this[0] = value
        }
    override var y
        get() = this[1]
        set(value) {
            this[1] = value
        }
    override var z
        get() = this[2]
        set(value) {
            this[2] = value
        }
    override var w
        get() = this[3]
        set(value) {
            this[3] = value
        }

    val array: FloatArray
        get() = fields

    constructor() : this(0f, 0f, 0f, 0f)
    constructor(f: Float) : this(f, f, f, f)
    constructor(xyz: Vec3f, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    constructor(other: Vec4f) : this(other.x, other.y, other.z, other.w)

    fun add(other: Vec4f): MutableVec4f {
        x += other.x
        y += other.y
        z += other.z
        w += other.w
        return this
    }

    fun mul(other: Vec4f): MutableVec4f {
        x *= other.x
        y *= other.y
        z *= other.z
        w *= other.w
        return this
    }

    fun norm(): MutableVec4f = scale(1f / length())

    fun quatMul(otherQuat: Vec4f): MutableVec4f {
        val px = w * otherQuat.x + x * otherQuat.w + y * otherQuat.z - z * otherQuat.y
        val py = w * otherQuat.y + y * otherQuat.w + z * otherQuat.x - x * otherQuat.z
        val pz = w * otherQuat.z + z * otherQuat.w + x * otherQuat.y - y * otherQuat.x
        val pw = w * otherQuat.w - x * otherQuat.x - y * otherQuat.y - z * otherQuat.z
        set(px, py, pz, pw)
        return this
    }

    fun conjugate(): MutableVec4f {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun scale(factor: Float): MutableVec4f {
        x *= factor
        y *= factor
        z *= factor
        w *= factor
        return this
    }

    fun set(x: Float, y: Float, z: Float, w: Float): MutableVec4f {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(other: Vec4f): MutableVec4f {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        return this
    }

    fun set(xyz: Vec3f, w: Float = 0f): MutableVec4f {
        x = xyz.x
        y = xyz.y
        z = xyz.z
        this.w = w
        return this
    }

    fun setEuler(pitch: Angle, yaw: Angle, roll: Angle): MutableVec4f {
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

        x =
            (chy_shp * chr) + (shy_chp * shr) // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        y =
            (shy_chp * chr) - (chy_shp * shr) // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        z =
            (chy_chp * shr) - (shy_shp * chr) // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        w =
            (chy_chp * chr) + (shy_shp * shr) // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return this
    }

    fun subtract(other: Vec4f): MutableVec4f {
        x -= other.x
        y -= other.y
        z -= other.z
        w -= other.w
        return this
    }

    operator fun plusAssign(other: Vec4f) {
        add(other)
    }

    operator fun minusAssign(other: Vec4f) {
        subtract(other)
    }

    open operator fun set(i: Int, v: Float) {
        fields[i] = v
    }
}

open class Vec4i(x: Int, y: Int, z: Int, w: Int) {

    protected val fields = IntArray(4)

    open val x get() = this[0]
    open val y get() = this[1]
    open val z get() = this[2]
    open val w get() = this[3]

    constructor(f: Int) : this(f, f, f, f)
    constructor(v: Vec4i) : this(v.x, v.y, v.z, v.w)

    init {
        fields[0] = x
        fields[1] = y
        fields[2] = z
        fields[3] = w
    }

    open operator fun get(i: Int): Int = fields[i]

    override fun toString(): String = "($x, $y, $z, $w)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec4i) return false

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
        val ZERO = Vec4i(0)
        val X_AXIS = Vec4i(1, 0, 0, 0)
        val Y_AXIS = Vec4i(0, 1, 0, 0)
        val Z_AXIS = Vec4i(0, 0, 1, 0)
        val W_AXIS = Vec4i(0, 0, 0, 1)
        val NEG_X_AXIS = Vec4i(-1, 0, 0, 0)
        val NEG_Y_AXIS = Vec4i(0, -1, 0, 0)
        val NEG_Z_AXIS = Vec4i(0, 0, -1, 0)
        val NEG_W_AXIS = Vec4i(0, 0, 0, -1)
    }
}

open class MutableVec4i(x: Int, y: Int, z: Int, w: Int) : Vec4i(x, y, z, w) {

    override var x
        get() = this[0]
        set(value) {
            this[0] = value
        }
    override var y
        get() = this[1]
        set(value) {
            this[1] = value
        }
    override var z
        get() = this[2]
        set(value) {
            this[2] = value
        }
    override var w
        get() = this[3]
        set(value) {
            this[3] = value
        }

    val array: IntArray
        get() = fields

    constructor() : this(0, 0, 0, 0)
    constructor(f: Int) : this(f, f, f, f)
    constructor(other: Vec4i) : this(other.x, other.y, other.z, other.w)

    init {
        fields[0] = x
        fields[1] = y
        fields[2] = z
        fields[3] = w
    }

    fun set(x: Int, y: Int, z: Int, w: Int): MutableVec4i {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(other: Vec4i): MutableVec4i {
        x = other.x
        y = other.y
        z = other.z
        w = other.w
        return this
    }

    open operator fun set(i: Int, v: Int) {
        fields[i] = v
    }

    fun add(other: Vec4i): MutableVec4i {
        x += other.x
        y += other.y
        z += other.z
        w += other.w
        return this
    }

    fun subtract(other: Vec4i): MutableVec4i {
        x -= other.x
        y -= other.y
        z -= other.z
        w -= other.w
        return this
    }
}