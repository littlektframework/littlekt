package com.lehaine.littlekt.math

import com.lehaine.littlekt.file.FloatBuffer
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.internal.lock
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 3x3 column major matrix.
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Mat3 {

    val data = FloatArray(9)

    var m00: Float
        get() = data[0]
        set(value) {
            data[0] = value
        }
    var m01: Float
        get() = data[3]
        set(value) {
            data[3] = value
        }
    var m02: Float
        get() = data[6]
        set(value) {
            data[6] = value
        }
    var m10: Float
        get() = data[1]
        set(value) {
            data[1] = value
        }
    var m11: Float
        get() = data[4]
        set(value) {
            data[4] = value
        }
    var m12: Float
        get() = data[7]
        set(value) {
            data[7] = value
        }
    var m20: Float
        get() = data[2]
        set(value) {
            data[2] = value
        }
    var m21: Float
        get() = data[5]
        set(value) {
            data[5] = value
        }
    var m22: Float
        get() = data[8]
        set(value) {
            data[8] = value
        }


    val rotation: Angle get() = atan2(m10, m00).radians

    /**
     * Note: You must clone or copy the values of this [Vec2f] due to the values of this referenced [Vec2f] will change.
     * @return this matrix's translation component
     */
    val position: Vec2f
        get() = getTranslation(tempVec2f)

    /**
     * Note: You must clone or copy the values of this [Vec2f] due to the values of this referenced [Vec2f] will change.
     * @return this matrix's scale component
     */
    val scale: Vec2f
        get() = getScale(tempVec2f)

    init {
        setToIdentity()
    }

    /**
     * Post-multiplies this matrix by a translation matrix.
     * @param x the X component of the translation vector
     * @param y the Y component of the translation vector
     * @return this matrix
     */
    fun translate(x: Float, y: Float): Mat3 {
        for (i in 0..2) {
            data[6 + i] += data[i] * x + data[3 + i] * y
        }
        return this
    }

    /**
     * Post-multiplies this matrix by a translation matrix.
     * @param offset the translation vector to add to the current matrix
     * @return this matrix
     */
    fun translate(offset: Vec2f): Mat3 = translate(offset.x, offset.y)

    /**
     * Post-multiplies this matrix by a translation matrix and stores the result in the specified matrix
     * @param x the X component of the translation vector
     * @param y the Y component of the translation vector
     * @return the [result] matrix
     */
    fun translate(x: Float, y: Float, result: Mat3): Mat3 {
        for (i in 0..8) {
            result.data[i] = data[i]
        }
        for (i in 0..2) {
            result.data[6 + i] += data[i] * x + data[3 + i] * y
        }
        return result
    }

    fun rotate(ax: Float, ay: Float, az: Float, degrees: Float): Mat3 {
        return lock(tmpMatLock) {
            tmpMatA.setToRotation(ax, ay, az, degrees)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(axis: Vec3f, degrees: Float) = rotate(axis.x, axis.y, axis.z, degrees)

    fun rotate(eulerX: Float, eulerY: Float, eulerZ: Float): Mat3 {
        return lock(tmpMatLock) {
            tmpMatA.setFromEulerAngles(eulerX, eulerY, eulerZ)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(ax: Float, ay: Float, az: Float, degrees: Float, result: Mat3): Mat3 {
        result.set(this)
        result.rotate(ax, ay, az, degrees)
        return result
    }

    fun rotate(axis: Vec3f, degrees: Float, result: Mat3) = rotate(axis.x, axis.y, axis.z, degrees, result)

    fun rotate(eulerX: Float, eulerY: Float, eulerZ: Float, result: Mat3): Mat3 {
        result.set(this)
        result.rotate(eulerX, eulerY, eulerZ)
        return result
    }

    fun transpose(): Mat3 {
        var d = this[1]
        this[1] = this[3]
        this[3] = d
        d = this[2]
        this[2] = this[6]
        this[6] = d
        d = this[5]
        this[5] = this[7]
        this[7] = d
        return this
    }

    fun transpose(result: Mat3): Mat3 {
        result[0] = this[0]
        result[1] = this[3]
        result[2] = this[6]

        result[3] = this[1]
        result[4] = this[4]
        result[5] = this[7]

        result[6] = this[2]
        result[7] = this[5]
        result[8] = this[8]

        return result
    }

    fun invert(): Boolean {
        return lock(tmpMatLock) { invert(tmpMatA).also { if (it) set(tmpMatA) } }
    }

    fun invert(result: Mat3): Boolean {
        var det = 0f
        for (i in 0..2) {
            det += (this[i] * (this[3 + (i + 1) % 3] * this[6 + (i + 2) % 3] - this[3 + (i + 2) % 3] * this[6 + (i + 1) % 3]))
        }

        return if (det > 0f) {
            det = 1f / det
            for (j in 0..2) {
                for (i in 0..2) {
                    result[j * 3 + i] =
                        ((this[((i + 1) % 3) * 3 + (j + 1) % 3] * this[((i + 2) % 3) * 3 + (j + 2) % 3]) -
                                (this[((i + 1) % 3) * 3 + (j + 2) % 3] * this[((i + 2) % 3) * 3 + (j + 1) % 3])) * det
                }
            }
            true
        } else {
            false
        }
    }

    fun transform(vec: MutableVec3f): MutableVec3f {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2]
        val z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2]
        return vec.set(x, y, z)
    }

    fun transform(vec: Vec3f, result: MutableVec3f): MutableVec3f {
        result.x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2]
        result.y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2]
        result.z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2]
        return result
    }

    fun transform(vec: MutableVec2f): MutableVec2f {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + this[0, 2]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + this[1, 2]
        return vec.set(x, y)
    }

    fun transform(vec: Vec2f, result: MutableVec2f): MutableVec2f {
        result.x = vec.x * this[0, 0] + vec.y * this[0, 1] + this[0, 2]
        result.y = vec.x * this[1, 0] + vec.y * this[1, 1] + this[1, 2]
        return result
    }

    /**
     * Post-multiplies this matrix with the given matrix, storing the result in this matrix.
     *
     * For example:
     * ```
     * A.mul(B) results in A := AB
     * ```
     * @param other the other matrix to multiply by
     * @return this matrix
     */
    fun mul(other: Mat3): Mat3 {
        return lock(tmpMatLock) {
            mul(other, tmpMatA)
            set(tmpMatA)
        }
    }

    /**
     * Post-multiplies this matrix with the given matrix, storing the result in the specified matrix.
     *
     * For example:
     * ```
     * A.mul(B) results in A := AB
     * ```
     * @param other the other matrix to multiply by
     * @param result the matrix to store the result
     * @return the [result] matrix
     */
    fun mul(other: Mat3, result: Mat3): Mat3 {
        for (i in 0..2) {
            for (j in 0..2) {
                var x = 0f
                for (k in 0..2) {
                    x += this[j + k * 3] * other[i * 3 + k]
                }
                result[i * 3 + j] = x
            }
        }
        return result
    }

    /**
     * Pre-multiplies this matrix with the given matrix, storing the result in this matrix.
     *
     * For example:
     * ```
     * A.mulLeft(B) results in A := BA.
     * ```
     * @param other the other matrix to multiply by
     * @return this matrix
     */
    fun mulLeft(other: Mat3): Mat3 {
        return lock(tmpMatLock) {
            mulLeft(other, tmpMatA)
            set(tmpMatA)
        }
    }

    /**
     * Pre-multiplies this matrix with the given matrix, storing the result in the specified matrix.
     *
     * For example:
     * ```
     * A.mulLeft(B) results in A := BA.
     * ```
     * @param other the other matrix to multiply by
     * @param result the matrix to store the result
     * @return the [result] matrix
     */
    fun mulLeft(other: Mat3, result: Mat3): Mat3 {
        for (i in 0..2) {
            for (j in 0..2) {
                var x = 0f
                for (k in 0..2) {
                    x += other[j + k * 3] * this[i * 3 + k]
                }
                result[i * 3 + j] = x
            }
        }
        return result
    }

    fun scale(sx: Float, sy: Float): Mat3 {
        for (i in 0..2) {
            data[i] *= sx
            data[3 + i] *= sy
        }
        return this
    }

    fun scale(scale: Vec2f): Mat3 = scale(scale.x, scale.y)

    fun scale(sx: Float, sy: Float, result: Mat3): Mat3 {
        for (i in 0..2) {
            result.data[i] = data[i] * sx
            result.data[3 + i] = data[3 + i] * sy
        }
        return result
    }

    fun set(other: Mat3): Mat3 {
        for (i in 0..8) {
            this[i] = other[i]
        }
        return this
    }

    fun set(floats: List<Float>) {
        for (i in 0..8) {
            this[i] = floats[i]
        }
    }

    fun setToIdentity(): Mat3 {
        for (i in 1..8) {
            this[i] = 0f
        }
        for (i in 0..8 step 4) {
            this[i] = 1f
        }
        return this
    }

    fun setToTranslate(x: Float, y: Float): Mat3 {
        setToIdentity()
        m02 = x
        m12 = y
        return this
    }

    fun setToTranslate(offset: Vec2f) = setToTranslate(offset.x, offset.y)

    fun setToRotation(angle: Angle): Mat3 {
        val cos = angle.cosine
        val sin = angle.sine
        m00 = cos
        m10 = sin
        m20 = 0f

        m01 = -sin
        m11 = cos
        m21 = 0f

        m02 = 0f
        m12 = 0f
        m22 = 1f

        return this
    }

    fun setFromEulerAngles(eulerX: Float, eulerY: Float, eulerZ: Float): Mat3 {
        val a = eulerX.toRad()
        val b = eulerY.toRad()
        val c = eulerZ.toRad()

        val ci = cos(a)
        val cj = cos(b)
        val ch = cos(c)
        val si = sin(a)
        val sj = sin(b)
        val sh = sin(c)
        val cc = ci * ch
        val cs = ci * sh
        val sc = si * ch
        val ss = si * sh

        data[0] = cj * ch
        data[3] = sj * sc - cs
        data[6] = sj * cc + ss

        data[1] = cj * sh
        data[4] = sj * ss + cc
        data[7] = sj * cs - sc

        data[2] = -sj
        data[5] = cj * si
        data[8] = cj * ci

        return this
    }

    fun setToRotation(ax: Float, ay: Float, az: Float, degrees: Float): Mat3 {
        var aX = ax
        var aY = ay
        var aZ = az
        val len = sqrt(aX * aX + aY * aY + aZ * aZ)
        if (!(1f - len).isFuzzyZero()) {
            val recipLen = 1f / len
            aX *= recipLen
            aY *= recipLen
            aZ *= recipLen
        }

        val radians = degrees.toRad()
        val sin = sin(radians)
        val cos = cos(radians)

        val nc = 1f - cos
        val xy = aX * aY
        val yz = aY * aZ
        val zx = aZ * aX
        val xs = aX * sin
        val ys = aY * sin
        val zs = aZ * sin

        this[0] = aX * aX * nc + cos
        this[3] = xy * nc - zs
        this[6] = zx * nc + ys
        this[1] = xy * nc + zs
        this[4] = aY * aY * nc + cos
        this[7] = yz * nc - xs
        this[2] = zx * nc - ys
        this[5] = yz * nc + xs
        this[8] = aZ * aZ * nc + cos

        return this
    }

    fun setToScale(sx: Float, sy: Float): Mat3 {
        setToIdentity()
        m00 = sx
        m11 = sy
        return this
    }

    fun setToScale(scale: Vec2f) = setToScale(scale.x, scale.y)

    fun getTranslation(result: MutableVec2f): MutableVec2f {
        result.x = m02
        result.y = m12
        return result
    }

    fun getScale(result: MutableVec2f): MutableVec2f {
        result.x = sqrt(m00 * m00 + m01 * m01)
        result.y = sqrt(m10 * m10 + m11 * m11)
        return result
    }

    operator fun get(i: Int): Float = data[i]

    operator fun get(row: Int, col: Int): Float = data[col * 3 + row]

    operator fun set(i: Int, value: Float) {
        data[i] = value
    }

    operator fun set(row: Int, col: Int, value: Float) {
        data[col * 3 + row] = value
    }

    fun setColVec(col: Int, vec: Vec3f) {
        this[0, col] = vec.x
        this[1, col] = vec.y
        this[2, col] = vec.z
    }

    fun getColVec(col: Int, result: MutableVec3f): MutableVec3f {
        result.x = this[0, col]
        result.y = this[1, col]
        result.z = this[2, col]
        return result
    }

    fun toBuffer(buffer: FloatBuffer): FloatBuffer {
        buffer.put(data, 0, 9)
        buffer.flip()
        return buffer
    }

    fun toList(): List<Float> {
        val list = mutableListOf<Float>()
        for (i in 0..8) {
            list += data[i]
        }
        return list
    }

    override fun toString(): String {
        return "[$m00|$m01|$m02]\n[$m10|$m11|$m12]\n[$m20|$m21|$m22]"
    }


    companion object {
        private val tmpMatLock = Any()
        private val tmpMatA = Mat3()
        private val tmpMatB = Mat3()
        private val tempVec2f = MutableVec2f()
    }
}