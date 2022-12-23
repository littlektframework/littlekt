package com.lehaine.littlekt.math

import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Vec3f internal constructor(x: Float, y: Float, z: Float, size: Int) : Vec2f(x, y, size) {

    open val z get() = this[2]

    constructor(x: Float, y: Float, z: Float) : this(x, y, z, 3)

    constructor(f: Float) : this(f, f, f)
    constructor(v: Vec3f) : this(v.x, v.y, v.z)

    init {
        fields[2] = z
    }

    fun add(other: Vec3f, result: MutableVec3f): MutableVec3f = result.set(this).add(other)

    fun cross(other: Vec3f, result: MutableVec3f): MutableVec3f {
        result.x = y * other.z - z * other.y
        result.y = z * other.x - x * other.z
        result.z = x * other.y - y * other.x
        return result
    }

    fun distance(other: Vec3f): Float = sqrt(sqrDistance(other))

    fun dot(other: Vec3f): Float = x * other.x + y * other.y + z * other.z

    /**
     * Checks vector components for equality using [com.lehaine.littlekt.math.isFuzzyEqual], that is all components must
     * have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec3f, eps: Float = FUZZY_EQ_F): Boolean =
        isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps) && isFuzzyEqual(z, other.z, eps)

    fun mix(other: Vec3f, weight: Float, result: MutableVec3f): MutableVec3f {
        result.x = other.x * weight + x * (1f - weight)
        result.y = other.y * weight + y * (1f - weight)
        result.z = other.z * weight + z * (1f - weight)
        return result
    }

    fun mul(other: Vec3f, result: MutableVec3f): MutableVec3f = result.set(this).mul(other)

    fun norm(result: MutableVec3f): MutableVec3f = result.set(this).norm()

    fun planeSpace(p: MutableVec3f, q: MutableVec3f) {
        if (abs(z) > SQRT_1_2) {
            // choose p in y-z plane
            val a = y * y + z * z
            val k = 1f / sqrt(a)
            p.x = 0f
            p.y = -z * k
            p.z = y * k
            // q = this x p
            q.x = a * k
            q.y = -x * p.z
            q.z = x * p.y
        } else {
            // choose p in x-y plane
            val a = x * x + y * y
            val k = 1f / sqrt(a)
            p.x = -y * k
            p.y = x * k
            p.z = 0f
            // q = this x p
            q.x = -z * p.y
            q.y = z * p.x
            q.z = a * k
        }
    }

    fun rotate(angle: Angle, axisX: Float, axisY: Float, axisZ: Float, result: MutableVec3f): MutableVec3f =
        result.set(this).rotate(angle, axisX, axisY, axisZ)

    fun rotate(angle: Angle, axis: Vec3f, result: MutableVec3f): MutableVec3f =
        result.set(this).rotate(angle, axis.x, axis.y, axis.z)

    fun scale(factor: Float, result: MutableVec3f): MutableVec3f = result.set(this).scale(factor)

    fun sqrDistance(other: Vec3f): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    override fun sqrLength(): Float = x * x + y * y + z * z

    fun subtract(other: Vec3f, result: MutableVec3f): MutableVec3f = result.set(this).subtract(other)

    operator fun times(other: Vec3f): Float = dot(other)

    fun toVec3(): Vec3f = Vec3f(x, y, z)

    fun toMutableVec3(): MutableVec3f = toMutableVec3(MutableVec3f())

    fun toMutableVec3(result: MutableVec3f): MutableVec3f = result.set(x, y, z)

    override fun toString(): String = "($x, $y, $z)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability consider using
     * [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3f) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    companion object {
        val ZERO = Vec3f(0f)
        val ONES = Vec3f(1f)
        val X_AXIS = Vec3f(1f, 0f, 0f)
        val Y_AXIS = Vec3f(0f, 1f, 0f)
        val Z_AXIS = Vec3f(0f, 0f, 1f)
        val NEG_X_AXIS = Vec3f(-1f, 0f, 0f)
        val NEG_Y_AXIS = Vec3f(0f, -1f, 0f)
        val NEG_Z_AXIS = Vec3f(0f, 0f, -1f)
    }
}

open class MutableVec3f(x: Float, y: Float, z: Float) : Vec3f(x, y, z) {

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

    val array: FloatArray
        get() = fields

    constructor() : this(0f, 0f, 0f)
    constructor(f: Float) : this(f, f, f)
    constructor(v: Vec3f) : this(v.x, v.y, v.z)

    fun add(other: Vec3f): MutableVec3f {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun add(x: Float, y: Float, z: Float): MutableVec3f {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun mul(other: Vec3f): MutableVec3f {
        x *= other.x
        y *= other.y
        z *= other.z
        return this
    }

    fun mul(matrix: Mat4): MutableVec3f {
        val newX = x * matrix[0] + y * matrix[4] + z * matrix[8] + matrix[12]
        val newY = x * matrix[1] + y * matrix[5] + z * matrix[9] + matrix[13]
        val newZ = x * matrix[2] + y * matrix[6] + z * matrix[10] + matrix[14]
        x = newX
        y = newY
        z = newZ
        return this
    }

    fun norm(): MutableVec3f {
        val l = length()
        return if (l != 0f) {
            scale(1f / l)
        } else {
            set(ZERO)
        }
    }

    fun cross(other: Vec3f): MutableVec3f = cross(other, this)

    fun rotate(angle: Angle, axisX: Float, axisY: Float, axisZ: Float): MutableVec3f {
        val rad = angle.radians
        val c = cos(rad)
        val c1 = 1f - c
        val s = sin(rad)

        val rx =
            x * (axisX * axisX * c1 + c) + y * (axisX * axisY * c1 - axisZ * s) + z * (axisX * axisZ * c1 + axisY * s)
        val ry =
            x * (axisY * axisX * c1 + axisZ * s) + y * (axisY * axisY * c1 + c) + z * (axisY * axisZ * c1 - axisX * s)
        val rz =
            x * (axisX * axisZ * c1 - axisY * s) + y * (axisY * axisZ * c1 + axisX * s) + z * (axisZ * axisZ * c1 + c)
        x = rx
        y = ry
        z = rz
        return this
    }

    fun rotate(angle: Angle, axis: Vec3f): MutableVec3f = rotate(angle, axis.x, axis.y, axis.z)

    fun scale(factor: Float): MutableVec3f {
        x *= factor
        y *= factor
        z *= factor
        return this
    }

    fun set(x: Float, y: Float, z: Float): MutableVec3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vec3f): MutableVec3f {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    fun subtract(other: Vec3f): MutableVec3f {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    operator fun divAssign(div: Float) {
        scale(1f / div)
    }

    operator fun minusAssign(other: Vec3f) {
        subtract(other)
    }

    operator fun plusAssign(other: Vec3f) {
        add(other)
    }

    open operator fun set(i: Int, v: Float) {
        fields[i] = v
    }

    operator fun timesAssign(factor: Float) {
        scale(factor)
    }
}

open class Vec3i(x: Int, y: Int, z: Int) {

    protected val fields = IntArray(3)

    open val x get() = this[0]
    open val y get() = this[1]
    open val z get() = this[2]

    constructor(f: Int) : this(f, f, f)
    constructor(v: Vec3i) : this(v.x, v.y, v.z)

    init {
        fields[0] = x
        fields[1] = y
        fields[2] = z
    }

    open operator fun get(i: Int): Int = fields[i]

    override fun toString(): String = "($x, $y, $z)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3i) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    companion object {
        val ZERO = Vec3i(0)
        val X_AXIS = Vec3i(1, 0, 0)
        val Y_AXIS = Vec3i(0, 1, 0)
        val Z_AXIS = Vec3i(0, 0, 1)
        val NEG_X_AXIS = Vec3i(-1, 0, 0)
        val NEG_Y_AXIS = Vec3i(0, -1, 0)
        val NEG_Z_AXIS = Vec3i(0, 0, -1)
    }
}

open class MutableVec3i(x: Int, y: Int, z: Int) : Vec3i(x, y, z) {

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

    val array: IntArray
        get() = fields

    constructor() : this(0, 0, 0)
    constructor(f: Int) : this(f, f, f)
    constructor(other: Vec3i) : this(other.x, other.y, other.z)

    init {
        fields[0] = x
        fields[1] = y
        fields[2] = z
    }

    fun set(x: Int, y: Int, z: Int): MutableVec3i {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vec3i): MutableVec3i {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    open operator fun set(i: Int, v: Int) {
        fields[i] = v
    }

    fun add(other: Vec3i): MutableVec3i {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun add(x: Int, y: Int, z: Int): MutableVec3i {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun subtract(other: Vec3i): MutableVec3i {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }
}
