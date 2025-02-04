package com.littlekt.math

import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Vec4f internal constructor(x: Float, y: Float, z: Float, w: Float, size: Int) :
    Vec3f(x, y, z, size) {
    open val w
        get() = this[3]

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
     * Checks vector components for equality using [com.littlekt.math.isFuzzyEqual], that is all
     * components must have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec4f, eps: Float = FUZZY_EQ_F): Boolean =
        isFuzzyEqual(x, other.x, eps) &&
            isFuzzyEqual(y, other.y, eps) &&
            isFuzzyEqual(z, other.z, eps) &&
            isFuzzyEqual(w, other.w, eps)

    fun mul(other: Vec4f, result: MutableVec4f): MutableVec4f = result.set(this).mul(other)

    fun norm(result: MutableVec4f): MutableVec4f = result.set(this).norm()

    fun scale(factor: Float, result: MutableVec4f): MutableVec4f = result.set(this).scale(factor)

    fun sqrDistance(other: Vec4f): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        val dw = z - other.w
        return dx * dx + dy * dy + dz * dz + dw * dw
    }

    override fun sqrLength(): Float = x * x + y * y + z * z + w * w

    fun subtract(other: Vec4f, result: MutableVec4f): MutableVec4f =
        result.set(this).subtract(other)

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

    fun toQuaternion(): Quaternion = Quaternion(x, y, z, w)

    fun toMutableQuaternion(): MutableQuaternion = MutableQuaternion(x, y, z, w)

    override fun toString(): String = "($x, $y, $z, $w)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability
     * consider using [isFuzzyEqual].
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

    val fields = IntArray(4)

    open val x
        get() = this[0]

    open val y
        get() = this[1]

    open val z
        get() = this[2]

    open val w
        get() = this[3]

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
