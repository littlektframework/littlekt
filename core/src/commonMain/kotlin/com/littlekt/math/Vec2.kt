package com.littlekt.math

import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.radians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Vec2f internal constructor(x: Float, y: Float, size: Int) {

    val fields = FloatArray(size)

    open val x
        get() = this[0]

    open val y
        get() = this[1]

    constructor(x: Float, y: Float) : this(x, y, 2)

    constructor(f: Float) : this(f, f)

    constructor(v: Vec2f) : this(v.x, v.y)

    init {
        fields[0] = x
        fields[1] = y
    }

    fun add(other: Vec2f, result: MutableVec2f): MutableVec2f = result.set(this).add(other)

    fun distance(other: Vec2f): Float = sqrt(sqrDistance(other))

    fun dot(other: Vec2f): Float = x * other.x + y * other.y

    /**
     * Checks vector components for equality using [com.littlekt.math.isFuzzyEqual], that is all
     * components must have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec2f, eps: Float = FUZZY_EQ_F): Boolean =
        isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps)

    fun length(): Float = sqrt(sqrLength())

    fun setLength(length: Float, result: MutableVec2f): MutableVec2f =
        result.set(this).setSqrLength(length * length)

    fun setSqrLength(sqrLength: Float, result: MutableVec2f): MutableVec2f =
        result.set(this).setSqrLength(sqrLength)

    fun mix(other: Vec2f, weight: Float, result: MutableVec2f): MutableVec2f {
        result.x = other.x * weight + x * (1f - weight)
        result.y = other.y * weight + y * (1f - weight)
        return result
    }

    fun mul(other: Vec2f, result: MutableVec2f): MutableVec2f = result.set(this).mul(other)

    fun norm(result: MutableVec2f): MutableVec2f = result.set(this).norm()

    fun rotate(angle: Angle, result: MutableVec2f): MutableVec2f = result.set(this).rotate(angle)

    fun scale(factor: Float, result: MutableVec2f): MutableVec2f = result.set(this).scale(factor)

    fun sqrDistance(other: Vec2f): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    open fun sqrLength(): Float = x * x + y * y

    fun subtract(other: Vec2f, result: MutableVec2f): MutableVec2f =
        result.set(this).subtract(other)

    fun angleTo(other: Vec2f): Angle {
        return atan2(other.x * y - other.y * x, x * other.x + y * other.y).radians
    }

    open operator fun get(i: Int): Float = fields[i]

    operator fun times(other: Vec2f): Float = dot(other)

    override fun toString(): String = "($x, $y)"

    fun toVec2(): Vec2f = Vec2f(x, y)

    fun toMutableVec2(): MutableVec2f = toMutableVec2(MutableVec2f())

    fun toMutableVec2(result: MutableVec2f): MutableVec2f = result.set(x, y)

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability
     * consider using [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2f) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {
        val ZERO = Vec2f(0f)
        val X_AXIS = Vec2f(1f, 0f)
        val Y_AXIS = Vec2f(0f, 1f)
        val NEG_X_AXIS = Vec2f(-1f, 0f)
        val NEG_Y_AXIS = Vec2f(0f, -1f)
    }
}

open class MutableVec2f(x: Float, y: Float) : Vec2f(x, y) {

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

    val array: FloatArray
        get() = fields

    constructor() : this(0f, 0f)

    constructor(f: Float) : this(f, f)

    constructor(v: Vec2f) : this(v.x, v.y)

    fun setLength(length: Float): MutableVec2f = setSqrLength(length * length)

    fun setSqrLength(sqrLength: Float): MutableVec2f {
        val oldLength = sqrLength()
        return if (oldLength == 0f || oldLength == sqrLength) {
            this
        } else {
            scale(sqrt(sqrLength / oldLength))
        }
    }

    fun add(other: Vec2f): MutableVec2f {
        x += other.x
        y += other.y
        return this
    }

    fun add(x: Float, y: Float): MutableVec2f {
        this.x += x
        this.y += y
        return this
    }

    fun mul(other: Vec2f): MutableVec2f {
        x *= other.x
        y *= other.y
        return this
    }

    fun mul(matrix: Mat3): MutableVec2f {
        val newX = x * matrix[0] + y * matrix[3] + matrix[6]
        val newY = x * matrix[1] + y * matrix[4] + matrix[7]
        x = newX
        y = newY
        return this
    }

    fun mul(matrix: Mat4): MutableVec2f {
        val newX = x * matrix[0] + y * matrix[4] + matrix[12]
        val newY = x * matrix[1] + y * matrix[5] + matrix[13]
        x = newX
        y = newY
        return this
    }

    fun mulAdd(other: Vec2f, scalar: Float): MutableVec2f {
        x += other.x * scalar
        y += other.y * scalar
        return this
    }

    fun mulAdd(other: Vec2f, scalar: Vec2f): MutableVec2f {
        x += other.x * scalar.x
        y += other.y * scalar.y
        return this
    }

    fun norm(): MutableVec2f {
        val length = length()
        if (length != 0f) {
            scale(1f / length)
        }
        return this
    }

    fun lerp(target: Vec2f, alpha: Float): MutableVec2f {
        val invAlpha = 1f - alpha
        x = (x * invAlpha) + (target.x * alpha)
        y = (y * invAlpha) + (target.y * alpha)
        return this
    }

    fun rotate(angle: Angle): MutableVec2f {
        val rad = angle.radians
        val cos = cos(rad)
        val sin = sin(rad)
        val rx = x * cos - y * sin
        val ry = x * sin + y * cos
        x = rx
        y = ry
        return this
    }

    fun scale(factor: Float): MutableVec2f {
        x *= factor
        y *= factor
        return this
    }

    fun scale(factor: Vec2f): MutableVec2f {
        x *= factor.x
        y *= factor.y
        return this
    }

    fun scale(xFactor: Float, yFactor: Float): MutableVec2f {
        x *= xFactor
        y *= yFactor
        return this
    }

    fun set(x: Float, y: Float): MutableVec2f {
        this.x = x
        this.y = y
        return this
    }

    fun set(other: Vec2f): MutableVec2f {
        x = other.x
        y = other.y
        return this
    }

    fun subtract(other: Vec2f): MutableVec2f {
        x -= other.x
        y -= other.y
        return this
    }

    fun subtract(x: Float, y: Float): MutableVec2f {
        this.x -= x
        this.y -= y
        return this
    }

    operator fun divAssign(div: Float) {
        scale(1f / div)
    }

    operator fun divAssign(other: Vec2f) {
        scale(1f / other.x, 1f / other.y)
    }

    operator fun minusAssign(other: Vec2f) {
        subtract(other)
    }

    operator fun plusAssign(other: Vec2f) {
        add(other)
    }

    open operator fun set(i: Int, v: Float) {
        fields[i] = v
    }

    operator fun timesAssign(factor: Float) {
        scale(factor)
    }
}

open class Vec2i(x: Int, y: Int) {

    val fields = IntArray(2)

    open val x
        get() = this[0]

    open val y
        get() = this[1]

    constructor(f: Int) : this(f, f)

    constructor(v: Vec2i) : this(v.x, v.y)

    init {
        fields[0] = x
        fields[1] = y
    }

    open operator fun get(i: Int): Int = fields[i]

    override fun toString(): String = "($x, $y)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2i) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {
        val ZERO = Vec2i(0)
        val X_AXIS = Vec2i(1, 0)
        val Y_AXIS = Vec2i(0, 1)
        val NEG_X_AXIS = Vec2i(-1, 0)
        val NEG_Y_AXIS = Vec2i(0, -1)
    }
}

open class MutableVec2i(x: Int, y: Int) : Vec2i(x, y) {

    override var x
        get() = this[0]
        set(value) {
            fields[0] = value
        }

    override var y
        get() = this[1]
        set(value) {
            fields[1] = value
        }

    val array: IntArray
        get() = fields

    constructor() : this(0, 0)

    constructor(f: Int) : this(f, f)

    constructor(other: Vec2i) : this(other.x, other.y)

    init {
        fields[0] = x
        fields[1] = y
    }

    fun set(other: Vec2i): MutableVec2i {
        x = other.x
        y = other.y
        return this
    }

    /**
     * Set the field based on index. Example:
     * ```
     * val vec2 = MutableVec2i()
     * vec2[0] = 3 // x
     * vec2[1] = 5 // y
     * ```
     */
    open operator fun set(i: Int, v: Int) {
        fields[i] = v
    }

    /**
     * @param x the new x value
     * @param y the new y value
     */
    fun setAll(x: Int, y: Int) {
        fields[0] = x
        fields[1] = y
    }

    fun add(other: Vec2i): MutableVec2i {
        x += other.x
        y += other.y
        return this
    }

    fun subtract(other: Vec2i): MutableVec2i {
        x -= other.x
        y -= other.y
        return this
    }
}
