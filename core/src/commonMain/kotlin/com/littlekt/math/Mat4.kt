package com.littlekt.math

import com.littlekt.file.FloatBuffer
import com.littlekt.math.geom.Angle
import com.littlekt.util.internal.lock
import kotlin.math.*

/**
 * A 4x4 column major matrix.
 *
 * @author Colton Daily
 * @date 11/23/2021
 */
open class Mat4 {

    val data = FloatArray(16)

    /**
     * XX: Typically the unrotated X component for scaling, also the cosine of the angle when
     * rotated on the Y and/or Z axis. On Vector3 multiplication this value is multiplied with the
     * source X component and added to the target X component.
     */
    var m00: Float
        get() = data[0]
        set(value) {
            data[0] = value
        }

    /**
     * XY: Typically the negative sine of the angle when rotated on the Z axis. On Vector3
     * multiplication this value is multiplied with the source Y component and added to the target X
     * component.
     */
    var m01: Float
        get() = data[4]
        set(value) {
            data[4] = value
        }

    /**
     * XZ: Typically the sine of the angle when rotated on the Y axis. On Vector3 multiplication
     * this value is multiplied with the source Z component and added to the target X component.
     */
    var m02: Float
        get() = data[8]
        set(value) {
            data[8] = value
        }

    /**
     * XW: Typically the translation of the X component. On Vector3 multiplication this value is
     * added to the target X component.
     */
    var m03: Float
        get() = data[12]
        set(value) {
            data[12] = value
        }

    /**
     * YX: Typically the sine of the angle when rotated on the Z axis. On Vector3 multiplication
     * this value is multiplied with the source X component and added to the target Y component.
     */
    var m10: Float
        get() = data[1]
        set(value) {
            data[1] = value
        }

    /**
     * YY: Typically the unrotated Y component for scaling, also the cosine of the angle when
     * rotated on the X and/or Z axis. On Vector3 multiplication this value is multiplied with the
     * source Y component and added to the target Y component.
     */
    var m11: Float
        get() = data[5]
        set(value) {
            data[5] = value
        }

    /**
     * YZ: Typically the negative sine of the angle when rotated on the X axis. On Vector3
     * multiplication this value is multiplied with the source Z component and added to the target Y
     * component.
     */
    var m12: Float
        get() = data[9]
        set(value) {
            data[9] = value
        }

    /**
     * YW: Typically the translation of the Y component. On Vector3 multiplication this value is
     * added to the target Y component.
     */
    var m13: Float
        get() = data[13]
        set(value) {
            data[13] = value
        }

    /**
     * ZX: Typically the negative sine of the angle when rotated on the Y axis. On Vector3
     * multiplication this value is multiplied with the source X component and added to the target Z
     * component.
     */
    var m20: Float
        get() = data[2]
        set(value) {
            data[2] = value
        }

    /**
     * ZY: Typical the sine of the angle when rotated on the X axis. On Vector3 multiplication this
     * value is multiplied with the source Y component and added to the target Z component.
     */
    var m21: Float
        get() = data[6]
        set(value) {
            data[6] = value
        }

    /**
     * ZZ: Typically the unrotated Z component for scaling, also the cosine of the angle when
     * rotated on the X and/or Y axis. On Vector3 multiplication this value is multiplied with the
     * source Z component and added to the target Z component.
     */
    var m22: Float
        get() = data[10]
        set(value) {
            data[10] = value
        }

    /**
     * ZW: Typically the translation of the Z component. On Vector3 multiplication this value is
     * added to the target Z component.
     */
    var m23: Float
        get() = data[14]
        set(value) {
            data[14] = value
        }

    /** WX: Typically the value zero. On [Vec3f] multiplication this value is ignored. */
    var m30: Float
        get() = data[3]
        set(value) {
            data[3] = value
        }

    /** WY: Typically the value zero. On [Vec3f] multiplication this value is ignored. */
    var m31: Float
        get() = data[7]
        set(value) {
            data[7] = value
        }

    /** WZ: Typically the value zero. On [Vec3f] multiplication this value is ignored. */
    var m32: Float
        get() = data[11]
        set(value) {
            data[11] = value
        }

    /** WW: Typically the value one. On [Vec3f] multiplication this value is ignored. */
    var m33: Float
        get() = data[15]
        set(value) {
            data[15] = value
        }

    /** @return the determinant of this matrix */
    val det: Float
        get() =
            m30 * m21 * m12 * m03 - m20 * m31 * m12 * m03 - m30 * m11 * m22 * m03 +
                m10 * m31 * m22 * m03 +
                m20 * m11 * m32 * m03 - m10 * m21 * m32 * m03 - m30 * m21 * m02 * m13 +
                m20 * m31 * m02 * m13 +
                m30 * m01 * m22 * m13 - m00 * m31 * m22 * m13 - m20 * m01 * m32 * m13 +
                m00 * m21 * m32 * m13 +
                m30 * m11 * m02 * m23 - m10 * m31 * m02 * m23 - m30 * m01 * m12 * m23 +
                m00 * m31 * m12 * m23 +
                m10 * m01 * m32 * m23 - m00 * m11 * m32 * m23 - m20 * m11 * m02 * m33 +
                m10 * m21 * m02 * m33 +
                m20 * m01 * m12 * m33 - m00 * m21 * m12 * m33 - m10 * m01 * m22 * m33 +
                m00 * m11 * m22 * m33

    /** @return the determinant of the 3x3 upper left matrix */
    val det3x3: Float
        get() =
            m00 * m11 * m22 + m01 * m12 * m20 + m02 * m10 * m21 -
                m00 * m12 * m21 -
                m01 * m10 * m22 -
                m02 * m11 * m20

    /** @return the squared scale factor on the X axis */
    val scaleXSqr
        get() = m00 * m00 + m01 * m01 + m02 * m02

    /** @return the squared scale factor on the Y axis */
    val scaleYSqr
        get() = m10 * m10 + m11 * m11 + m12 * m12

    /** @return the squared scale factor on the Z axis */
    val scaleZSqr
        get() = m20 * m20 + m21 * m21 + m22 * m22

    /** @return the scale factor on the X axis (non-negative) */
    val scaleX
        get() = if (m01.isFuzzyZero() && m02.isFuzzyZero()) abs(m00) else sqrt(scaleXSqr)

    /** @return the scale factor on the Y axis (non-negative) */
    val scaleY
        get() = if (m10.isFuzzyZero() && m12.isFuzzyZero()) abs(m11) else sqrt(scaleYSqr)

    /** @return the scale factor on the X axis (non-negative) */
    val scaleZ
        get() = if (m20.isFuzzyZero() && m21.isFuzzyZero()) abs(m22) else sqrt(scaleZSqr)

    init {
        setToIdentity()
    }

    fun set(other: Mat4): Mat4 {
        for (i in 0..15) {
            data[i] = other.data[i]
        }
        return this
    }

    fun set(floats: List<Float>): Mat4 {
        for (i in 0..15) {
            data[i] = floats[i]
        }
        return this
    }

    fun set(other: Mat3): Mat4 {
        data[0] = other.data[0]
        data[1] = other.data[1]
        data[2] = other.data[2]
        data[3] = 0f
        data[4] = other.data[3]
        data[5] = other.data[4]
        data[6] = other.data[5]
        data[7] = 0f
        data[8] = 0f
        data[9] = 0f
        data[10] = 1f
        data[11] = 0f
        data[12] = other.data[6]
        data[13] = other.data[7]
        data[14] = 0f
        data[15] = other.data[8]
        return this
    }

    /**
     * Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param quaternion the quaternion that is to be used to set this matrix
     * @return this matrix
     */
    fun set(quaternion: Vec4f) = set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)

    /**
     * Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param qx The X component of the quaternion that is to be used to set this matrix.
     * @param qy The Y component of the quaternion that is to be used to set this matrix.
     * @param qz The Z component of the quaternion that is to be used to set this matrix.
     * @param qw The W component of the quaternion that is to be used to set this matrix.
     * @return this matrix
     */
    fun set(qx: Float, qy: Float, qz: Float, qw: Float) = set(0f, 0f, 0f, qx, qy, qz, qw)

    /**
     * Sets the matrix to a rotation matrix representing the translation, quaternion, and scale.
     *
     * @param translation the translation component
     * @param quaternion the rotation component
     * @param scale the scale component
     * @return this matrix
     */
    fun set(translation: Vec3f, quaternion: Vec4f, scale: Vec3f) =
        set(
            translation.x,
            translation.y,
            translation.z,
            quaternion.x,
            quaternion.y,
            quaternion.z,
            quaternion.w,
            scale.x,
            scale.y,
            scale.z
        )

    /**
     * Sets the matrix to a rotation matrix representing the translation, quaternion, and scale.
     *
     * @param tx The X component of the translation that is to be used to set this matrix.
     * @param ty The Y component of the translation that is to be used to set this matrix.
     * @param tz The Z component of the translation that is to be used to set this matrix.
     * @param qx The X component of the quaternion that is to be used to set this matrix.
     * @param qy The Y component of the quaternion that is to be used to set this matrix.
     * @param qz The Z component of the quaternion that is to be used to set this matrix.
     * @param qw The W component of the quaternion that is to be used to set this matrix.
     * @param sx The X component of the scaling that is to be used to set this matrix.
     * @param sy The Y component of the scaling that is to be used to set this matrix.
     * @param sz The Z component of the scaling that is to be used to set this matrix.
     * @return this matrix
     */
    fun set(
        tx: Float,
        ty: Float,
        tz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        sx: Float = 1f,
        sy: Float = 1f,
        sz: Float = 1f,
    ): Mat4 {
        var s = sqrt(qw * qw + qx * qx + qy * qy + qz * qz)
        s = 1f / (s * s)

        m00 = sx * (1 - 2 * s * (qy * qy + qz * qz))
        m01 = sy * (2 * s * (qx * qy - qz * qw))
        m02 = sz * (2 * s * (qx * qz + qy * qw))
        m03 = tx

        m10 = sx * (2 * s * (qx * qy + qz * qw))
        m11 = sy * (1 - 2 * s * (qx * qx + qz * qz))
        m12 = sz * (2 * s * (qy * qz - qx * qw))
        m12 = ty

        m20 = sx * (2 * s * (qx * qz - qy * qw))
        m21 = sy * (2 * s * (qy * qz + qx * qw))
        m22 = sz * (1 - 2 * s * (qx * qx + qy * qy))
        m23 = tz

        m30 = 0f
        m31 = 0f
        m32 = 0f
        m33 = 1f

        return this
    }

    fun set(xAxis: Vec3f, yAxis: Vec3f, zAxis: Vec3f, pos: Vec3f): Mat4 {
        m00 = xAxis.x
        m01 = xAxis.y
        m02 = xAxis.z
        m10 = yAxis.x
        m11 = yAxis.y
        m12 = yAxis.z
        m20 = zAxis.x
        m21 = zAxis.y
        m22 = zAxis.z
        m03 = pos.x
        m13 = pos.y
        m23 = pos.z
        m30 = 0f
        m31 = 0f
        m32 = 0f
        m33 = 1f
        return this
    }

    /**
     * Post-multiplies this matrix by a translation matrix.
     *
     * @param x the X component of the translation vector
     * @param y the Y component of the translation vector
     * @param z the Z component of the translation vector
     * @return this matrix
     */
    fun translate(x: Float, y: Float, z: Float): Mat4 {
        for (i in 0..3) {
            data[12 + i] += data[i] * x + data[4 + i] * y + data[8 + i] * z
        }
        return this
    }

    /**
     * Post-multiplies this matrix by a translation matrix.
     *
     * @param offset the translation vector to add to the current matrix
     * @return this matrix
     */
    fun translate(offset: Vec3f) = translate(offset.x, offset.y, offset.z)

    /**
     * Post-multiplies this matrix by a translation matrix and stores the result in the specified
     * matrix
     *
     * @param x the X component of the translation vector
     * @param y the Y component of the translation vector
     * @param z the Z component of the translation vector
     * @return the [result] matrix
     */
    fun translate(x: Float, y: Float, z: Float, result: Mat4): Mat4 {
        for (i in 0..11) {
            result.data[i] = data[i]
        }
        for (i in 0..3) {
            result.data[12 + i] = data[i] * x + data[4 + i] * y + data[8 + i] * z + data[12 + i]
        }
        return result
    }

    fun rotate(ax: Float, ay: Float, az: Float, angle: Angle): Mat4 {
        return lock(tmpMatLock) {
            tmpMatA.setToRotation(ax, ay, az, angle)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(axis: Vec3f, angle: Angle) = rotate(axis.x, axis.y, axis.z, angle)

    fun rotate(eulerX: Angle, eulerY: Angle, eulerZ: Angle): Mat4 {
        return lock(tmpMatLock) {
            tmpMatA.setFromEulerAngles(eulerX, eulerY, eulerZ)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(ax: Float, ay: Float, az: Float, angle: Angle, result: Mat4): Mat4 {
        return lock(tmpMatLock) {
            tmpMatA.setToRotation(ax, ay, az, angle)
            mul(tmpMatA, result)
        }
    }

    fun rotate(axis: Vec3f, angle: Angle, result: Mat4) =
        rotate(axis.x, axis.y, axis.z, angle, result)

    fun rotate(eulerX: Angle, eulerY: Angle, eulerZ: Angle, result: Mat4): Mat4 {
        result.set(this)
        result.rotate(eulerX, eulerY, eulerZ)
        return result
    }

    fun rotate(rotationMat: Mat3) {
        return lock(tmpMatLock) {
            tmpMatA.setToIdentity().set(rotationMat)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(quaternion: Vec4f) {
        return lock(tmpMatLock) {
            tmpMatA.setToIdentity().set(quaternion)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    /**
     * Post-multiplies this matrix with the given matrix, storing the result in this matrix.
     *
     * For example:
     * ```
     * A.mul(B) results in A := AB
     * ```
     *
     * @param other the other matrix to multiply by
     * @return this matrix
     */
    fun mul(other: Mat4): Mat4 {
        return lock(tmpMatLock) {
            mul(other, tmpMatA)
            set(tmpMatA)
        }
    }

    /**
     * Post-multiplies this matrix with the given matrix, storing the result in the specified
     * matrix.
     *
     * For example:
     * ```
     * A.mul(B) results in A := AB
     * ```
     *
     * @param other the other matrix to multiply by
     * @param result the matrix to store the result
     * @return the [result] matrix
     */
    fun mul(other: Mat4, result: Mat4): Mat4 {
        for (i in 0..3) {
            for (j in 0..3) {
                var x = 0f
                for (k in 0..3) {
                    x += data[j + k * 4] * other.data[i * 4 + k]
                }
                result.data[i * 4 + j] = x
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
     *
     * @param other the other matrix to multiply by
     * @return this matrix
     */
    fun mulLeft(other: Mat4): Mat4 {
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
     *
     * @param other the other matrix to multiply by
     * @param result the matrix to store the result
     * @return the [result] matrix
     */
    fun mulLeft(other: Mat4, result: Mat4): Mat4 {
        for (i in 0..3) {
            for (j in 0..3) {
                var x = 0f
                for (k in 0..3) {
                    x += other.data[j + k * 4] * data[i * 4 + k]
                }
                result.data[i * 4 + j] = x
            }
        }
        return result
    }

    fun transpose(): Mat4 {
        return lock(tmpMatLock) { set(transpose(tmpMatA)) }
    }

    fun transpose(result: Mat4): Mat4 {
        for (i in 0..3) {
            val mBase = i * 4
            result.data[i] = data[mBase]
            result.data[i + 4] = data[mBase + 1]
            result.data[i + 8] = data[mBase + 2]
            result.data[i + 12] = data[mBase + 3]
        }
        return result
    }

    fun transform(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + w * this[0, 3]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + w * this[1, 3]
        val z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + w * this[2, 3]
        return vec.set(x, y, z)
    }

    fun transform(vec: Vec3f, w: Float, result: MutableVec3f): MutableVec3f {
        result.x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + w * this[0, 3]
        result.y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + w * this[1, 3]
        result.z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + w * this[2, 3]
        return result
    }

    fun transform(vec: MutableVec4f): MutableVec4f {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + vec.w * this[0, 3]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + vec.w * this[1, 3]
        val z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + vec.w * this[2, 3]
        val w = vec.x * this[3, 0] + vec.y * this[3, 1] + vec.z * this[3, 2] + vec.w * this[3, 3]
        return vec.set(x, y, z, w)
    }

    fun transform(vec: Vec4f, result: MutableVec4f): MutableVec4f {
        result.x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + vec.w * this[0, 3]
        result.y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + vec.w * this[1, 3]
        result.z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + vec.w * this[2, 3]
        result.w = vec.x * this[3, 0] + vec.y * this[3, 1] + vec.z * this[3, 2] + vec.w * this[3, 3]
        return result
    }

    /**
     * Sets this matrix to all zeros.
     *
     * @return this matrix
     */
    fun setToZero(): Mat4 {
        for (i in 0..15) {
            data[i] = 0f
        }
        return this
    }

    /**
     * Sets this matrix to an identity matrix.
     *
     * @return this matrix
     */
    fun setToIdentity(): Mat4 {
        for (i in 1..15) {
            data[i] = 0f
        }
        m00 = 1f
        m11 = 1f
        m22 = 1f
        m33 = 1f
        return this
    }

    /**
     * Invert the matrix. Stores the result in this matrix.
     *
     * @param eps the value to check when checking if the matrix is singular.
     * @return this matrix
     * @throws RuntimeException if the matrix is singular (not invertible)
     */
    fun invert(eps: Float = 0f): Mat4 {
        return lock(tmpMatLock) { invert(tmpMatA, eps).also { set(tmpMatA) } }
    }

    /**
     * Invert the matrix. Stores the result in the specified [Mat4].
     *
     * @param result the matrix to store the result
     * @param eps the value to check when checking if the matrix is singular.
     * @return this matrix
     * @throws RuntimeException if the matrix is singular (not invertible)
     */
    fun invert(result: Mat4, eps: Float = 0f): Mat4 {
        // Invert a 4 x 4 matrix using Cramer's Rule

        // transpose matrix
        val src0 = data[0]
        val src4 = data[1]
        val src8 = data[2]
        val src12 = data[3]

        val src1 = data[4]
        val src5 = data[5]
        val src9 = data[6]
        val src13 = data[7]

        val src2 = data[8]
        val src6 = data[9]
        val src10 = data[10]
        val src14 = data[11]

        val src3 = data[12]
        val src7 = data[13]
        val src11 = data[14]
        val src15 = data[15]

        // calculate pairs for first 8 elements (cofactors)
        val atmp0 = src10 * src15
        val atmp1 = src11 * src14
        val atmp2 = src9 * src15
        val atmp3 = src11 * src13
        val atmp4 = src9 * src14
        val atmp5 = src10 * src13
        val atmp6 = src8 * src15
        val atmp7 = src11 * src12
        val atmp8 = src8 * src14
        val atmp9 = src10 * src12
        val atmp10 = src8 * src13
        val atmp11 = src9 * src12

        // calculate first 8 elements (cofactors)
        val dst0 =
            atmp0 * src5 + atmp3 * src6 + atmp4 * src7 -
                (atmp1 * src5 + atmp2 * src6 + atmp5 * src7)
        val dst1 =
            atmp1 * src4 + atmp6 * src6 + atmp9 * src7 -
                (atmp0 * src4 + atmp7 * src6 + atmp8 * src7)
        val dst2 =
            atmp2 * src4 + atmp7 * src5 + atmp10 * src7 -
                (atmp3 * src4 + atmp6 * src5 + atmp11 * src7)
        val dst3 =
            atmp5 * src4 + atmp8 * src5 + atmp11 * src6 -
                (atmp4 * src4 + atmp9 * src5 + atmp10 * src6)
        val dst4 =
            atmp1 * src1 + atmp2 * src2 + atmp5 * src3 -
                (atmp0 * src1 + atmp3 * src2 + atmp4 * src3)
        val dst5 =
            atmp0 * src0 + atmp7 * src2 + atmp8 * src3 -
                (atmp1 * src0 + atmp6 * src2 + atmp9 * src3)
        val dst6 =
            atmp3 * src0 + atmp6 * src1 + atmp11 * src3 -
                (atmp2 * src0 + atmp7 * src1 + atmp10 * src3)
        val dst7 =
            atmp4 * src0 + atmp9 * src1 + atmp10 * src2 -
                (atmp5 * src0 + atmp8 * src1 + atmp11 * src2)

        // calculate pairs for second 8 elements (cofactors)
        val btmp0 = src2 * src7
        val btmp1 = src3 * src6
        val btmp2 = src1 * src7
        val btmp3 = src3 * src5
        val btmp4 = src1 * src6
        val btmp5 = src2 * src5
        val btmp6 = src0 * src7
        val btmp7 = src3 * src4
        val btmp8 = src0 * src6
        val btmp9 = src2 * src4
        val btmp10 = src0 * src5
        val btmp11 = src1 * src4

        // calculate second 8 elements (cofactors)
        val dst8 =
            btmp0 * src13 + btmp3 * src14 + btmp4 * src15 -
                (btmp1 * src13 + btmp2 * src14 + btmp5 * src15)
        val dst9 =
            btmp1 * src12 + btmp6 * src14 + btmp9 * src15 -
                (btmp0 * src12 + btmp7 * src14 + btmp8 * src15)
        val dst10 =
            btmp2 * src12 + btmp7 * src13 + btmp10 * src15 -
                (btmp3 * src12 + btmp6 * src13 + btmp11 * src15)
        val dst11 =
            btmp5 * src12 + btmp8 * src13 + btmp11 * src14 -
                (btmp4 * src12 + btmp9 * src13 + btmp10 * src14)
        val dst12 =
            btmp2 * src10 + btmp5 * src11 + btmp1 * src9 -
                (btmp4 * src11 + btmp0 * src9 + btmp3 * src10)
        val dst13 =
            btmp8 * src11 + btmp0 * src8 + btmp7 * src10 -
                (btmp6 * src10 + btmp9 * src11 + btmp1 * src8)
        val dst14 =
            btmp6 * src9 + btmp11 * src11 + btmp3 * src8 -
                (btmp10 * src11 + btmp2 * src8 + btmp7 * src9)
        val dst15 =
            btmp10 * src10 + btmp4 * src8 + btmp9 * src9 -
                (btmp8 * src9 + btmp11 * src10 + btmp5 * src8)

        // calculate determinant
        val det = src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3

        if (det.isFuzzyZero(eps)) {
            throw RuntimeException("Un-invertible matrix")
        }

        // calculate matrix inverse
        val invdet = 1f / det
        result.data[0] = dst0 * invdet
        result.data[1] = dst1 * invdet
        result.data[2] = dst2 * invdet
        result.data[3] = dst3 * invdet

        result.data[4] = dst4 * invdet
        result.data[5] = dst5 * invdet
        result.data[6] = dst6 * invdet
        result.data[7] = dst7 * invdet

        result.data[8] = dst8 * invdet
        result.data[9] = dst9 * invdet
        result.data[10] = dst10 * invdet
        result.data[11] = dst11 * invdet

        result.data[12] = dst12 * invdet
        result.data[13] = dst13 * invdet
        result.data[14] = dst14 * invdet
        result.data[15] = dst15 * invdet

        return this
    }

    /**
     * Sets the matrix to an orthographic projection.
     *
     * @param left The left clipping plane
     * @param right The right clipping plane
     * @param bottom The bottom clipping plane
     * @param top The top clipping plane
     * @param near The near clipping plane
     * @param far The far clipping plane
     * @return this matrix
     */
    fun setToOrthographic(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        near: Float,
        far: Float
    ): Mat4 {
        if (left == right) {
            throw IllegalArgumentException("left == right")
        }
        if (bottom == top) {
            throw IllegalArgumentException("bottom == top")
        }
        if (near == far) {
            throw IllegalArgumentException("near == far")
        }

        val width = 1f / (right - left)
        val height = 1f / (top - bottom)
        val depth = 1f / (far - near)
        val x = 2f * width
        val y = 2f * height
        val z = -2f * depth
        val tx = -(right + left) * width
        val ty = -(top + bottom) * height
        val tz = (far + near) * depth
        m00 = x
        m10 = 0f
        m20 = 0f
        m30 = 0f
        m01 = 0f
        m11 = y
        m21 = 0f
        m31 = 0f
        m02 = 0f
        m12 = 0f
        m22 = z
        m32 = 0f
        m03 = tx
        m13 - ty
        m23 = tz
        m33 = 1f

        return this
    }

    /**
     * Sets the matrix to a perspective projection
     *
     * @param fovy the field of value of the height in degrees
     * @param aspect the "width over height" aspect ratio
     * @param near the near plane
     * @param far the far plane
     * @return this matrix
     */
    fun setToPerspective(fovy: Float, aspect: Float, near: Float, far: Float): Mat4 {
        val f = 1f / tan(fovy * (PI / 360.0)).toFloat()
        val rangeReciprocal = 1f / (near - far)

        data[0] = f / aspect
        data[1] = 0f
        data[2] = 0f
        data[3] = 0f

        data[4] = 0f
        data[5] = f
        data[6] = 0f
        data[7] = 0f

        data[8] = 0f
        data[9] = 0f
        data[10] = (far + near) * rangeReciprocal
        data[11] = -1f

        data[12] = 0f
        data[13] = 0f
        data[14] = 2f * far * near * rangeReciprocal
        data[15] = 0f

        return this
    }

    fun setToTranslate(translation: Vec3f) =
        setToTranslate(translation.x, translation.y, translation.z)

    fun setToTranslate(x: Float, y: Float, z: Float): Mat4 {
        setToIdentity()
        m03 = x
        m13 = y
        m23 = z
        return this
    }

    fun setToTranslateAndScaling(
        tx: Float,
        ty: Float,
        tz: Float,
        sx: Float,
        sy: Float,
        sz: Float
    ): Mat4 {
        setToIdentity()
        m03 = tx
        m13 = ty
        m23 = tz
        m00 = sx
        m11 = sy
        m22 = sz
        return this
    }

    fun setToTranslateAndScaling(translation: Vec3f, scale: Vec3f) =
        setToTranslateAndScaling(
            translation.x,
            translation.y,
            translation.z,
            scale.x,
            scale.y,
            scale.z
        )

    fun setToRotation(axis: Vec3f, angle: Angle): Mat4 =
        setToRotation(axis.x, axis.y, axis.z, angle)

    fun setToRotation(ax: Float, ay: Float, az: Float, angle: Angle): Mat4 {
        val a = angle.radians
        var x = ax
        var y = ay
        var z = az
        data[3] = 0f
        data[7] = 0f
        data[11] = 0f
        data[12] = 0f
        data[13] = 0f
        data[14] = 0f
        data[15] = 1f
        val s = sin(a)
        val c = cos(a)
        if (x > 0f && y == 0f && z == 0f) {
            data[5] = c
            data[10] = c
            data[6] = s
            data[9] = -s
            data[1] = 0f
            data[2] = 0f
            data[4] = 0f
            data[8] = 0f
            data[0] = 1f
        } else if (x == 0f && y > 0f && z == 0f) {
            data[0] = c
            data[10] = c
            data[8] = s
            data[2] = -s
            data[1] = 0f
            data[4] = 0f
            data[6] = 0f
            data[9] = 0f
            data[5] = 1f
        } else if (x == 0f && y == 0f && z > 0f) {
            data[0] = c
            data[5] = c
            data[1] = s
            data[4] = -s
            data[2] = 0f
            data[6] = 0f
            data[8] = 0f
            data[9] = 0f
            data[10] = 1f
        } else {
            val recipLen = 1f / sqrt(x * x + y * y + z * z)
            x *= recipLen
            y *= recipLen
            z *= recipLen

            val nc = 1f - c
            val xy = x * y
            val yz = y * z
            val zx = z * x
            val xs = x * s
            val ys = y * s
            val zs = z * s
            data[0] = x * x * nc + c
            data[4] = xy * nc - zs
            data[8] = zx * nc + ys
            data[1] = xy * nc + zs
            data[5] = y * y * nc + c
            data[9] = yz * nc - xs
            data[2] = zx * nc - ys
            data[6] = yz * nc + xs
            data[10] = z * z * nc + c
        }
        return this
    }

    fun setToRotation(quaternion: Vec4f): Mat4 {
        val r = quaternion.w
        val i = quaternion.x
        val j = quaternion.y
        val k = quaternion.z

        var s = sqrt(r * r + i * i + j * j + k * k)
        s = 1f / (s * s)

        this[0, 0] = 1 - 2 * s * (j * j + k * k)
        this[0, 1] = 2 * s * (i * j - k * r)
        this[0, 2] = 2 * s * (i * k + j * r)
        this[0, 3] = 0f

        this[1, 0] = 2 * s * (i * j + k * r)
        this[1, 1] = 1 - 2 * s * (i * i + k * k)
        this[1, 2] = 2 * s * (j * k - i * r)
        this[1, 3] = 0f

        this[2, 0] = 2 * s * (i * k - j * r)
        this[2, 1] = 2 * s * (j * k + i * r)
        this[2, 2] = 1 - 2 * s * (i * i + j * j)
        this[2, 3] = 0f

        this[3, 0] = 0f
        this[3, 1] = 0f
        this[3, 2] = 0f
        this[3, 3] = 1f

        return this
    }

    /**
     * Sets this matrix to a rotation matrix from the given euler angles.
     *
     * @param yaw the yaw
     * @param pitch the pitch
     * @param roll the roll
     * @return this matrix
     */
    fun setFromEulerAngles(yaw: Angle, pitch: Angle, roll: Angle): Mat4 {
        val a = yaw.radians
        val b = pitch.radians
        val c = roll.radians

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
        data[4] = sj * sc - cs
        data[8] = sj * cc + ss
        data[12] = 0f

        data[1] = cj * sh
        data[5] = sj * ss + cc
        data[9] = sj * cs - sc
        data[13] = 0f

        data[2] = -sj
        data[6] = cj * si
        data[10] = cj * ci
        data[14] = 0f

        data[3] = 0f
        data[7] = 0f
        data[11] = 0f
        data[15] = 1f

        return this
    }

    fun setToScaling(x: Float, y: Float, z: Float): Mat4 {
        setToIdentity()
        m00 = x
        m11 = y
        m22 = z
        return this
    }

    fun setToScaling(scale: Vec3f): Mat4 = setToScaling(scale.x, scale.y, scale.z)

    /**
     * Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a
     * translation matrix to get a camera model view matrix.
     *
     * @param direction the direction vector
     * @param up the up vector
     * @return this matrix
     */
    fun setToLookAt(direction: Vec3f, up: Vec3f): Mat4 {
        l_vez.set(direction).norm()
        l_vex.set(direction).norm().cross(up).norm()
        l_vey.set(l_vex).cross(l_vez).norm()
        setToIdentity()

        m00 = l_vex.x
        m01 = l_vex.y
        m02 = l_vex.z

        m10 = l_vey.x
        m11 = l_vey.y
        m12 = l_vey.z

        m20 = -l_vez.x
        m21 = -l_vez.y
        m22 = -l_vez.z

        return this
    }

    /**
     * Sets this matrix to a look at matrix with the given position, target and up vector.
     *
     * @param position the position
     * @param lookAt the target
     * @param up the up vector
     * @return this matrix
     */
    fun setToLookAt(position: Vec3f, lookAt: Vec3f, up: Vec3f): Mat4 {
        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:
        var fx = lookAt.x - position.x
        var fy = lookAt.y - position.y
        var fz = lookAt.z - position.z

        // Normalize f
        val rlf = 1f / sqrt(fx * fx + fy * fy + fz * fz)
        fx *= rlf
        fy *= rlf
        fz *= rlf

        // compute s = f x up (x means "cross product")
        var sx = fy * up.z - fz * up.y
        var sy = fz * up.x - fx * up.z
        var sz = fx * up.y - fy * up.x

        // and normalize s
        val rls = 1f / sqrt(sx * sx + sy * sy + sz * sz)
        sx *= rls
        sy *= rls
        sz *= rls

        // compute u = s x f
        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        data[0] = sx
        data[1] = ux
        data[2] = -fx
        data[3] = 0f

        data[4] = sy
        data[5] = uy
        data[6] = -fy
        data[7] = 0f

        data[8] = sz
        data[9] = uz
        data[10] = -fz
        data[11] = 0f

        data[12] = 0f
        data[13] = 0f
        data[14] = 0f
        data[15] = 1f

        return translate(-position.x, -position.y, -position.z)
    }

    fun setToWorld(position: Vec3f, forward: Vec3f, up: Vec3f): Mat4 {
        tmpForward.set(forward).norm()
        right.set(tmpForward).cross(up).norm()
        tmpUp.set(right).cross(tmpForward).norm()
        set(right, tmpUp, tmpForward.scale(-1f), position)
        return this
    }

    fun scale(x: Float, y: Float = x, z: Float = y): Mat4 {
        for (i in 0..3) {
            data[i] *= x
            data[4 + i] *= y
            data[8 + i] *= z
        }
        return this
    }

    fun scale(scale: Vec3f) = scale(scale.x, scale.y, scale.z)

    fun getTranslation(out: MutableVec3f): MutableVec3f {
        out.x = m03
        out.y = m13
        out.z = m23
        return out
    }

    fun getScale(out: MutableVec3f): MutableVec3f {
        return out.set(scaleX, scaleY, scaleZ)
    }

    operator fun get(i: Int): Float = data[i]

    operator fun get(row: Int, col: Int): Float = data[col * 4 + row]

    operator fun set(i: Int, value: Float) {
        data[i] = value
    }

    operator fun set(row: Int, col: Int, value: Float) {
        data[col * 4 + row] = value
    }

    fun setRow(row: Int, vec: Vec3f, w: Float) {
        this[row, 0] = vec.x
        this[row, 1] = vec.y
        this[row, 2] = vec.z
        this[row, 3] = w
    }

    fun setRow(row: Int, value: Vec4f) {
        this[row, 0] = value.x
        this[row, 1] = value.y
        this[row, 2] = value.z
        this[row, 3] = value.w
    }

    fun getRow(row: Int, result: MutableVec4f): MutableVec4f {
        result.x = this[row, 0]
        result.y = this[row, 1]
        result.z = this[row, 2]
        result.w = this[row, 3]
        return result
    }

    fun setCol(col: Int, vec: Vec3f, w: Float) {
        this[0, col] = vec.x
        this[1, col] = vec.y
        this[2, col] = vec.z
        this[3, col] = w
    }

    fun setCol(col: Int, vec: Vec4f) {
        this[0, col] = vec.x
        this[1, col] = vec.y
        this[2, col] = vec.z
        this[3, col] = vec.w
    }

    fun getCol(col: Int, result: MutableVec4f): MutableVec4f {
        result.x = this[0, col]
        result.y = this[1, col]
        result.z = this[2, col]
        result.w = this[3, col]
        return result
    }

    fun getOrigin(result: MutableVec3f): MutableVec3f {
        result.x = this[0, 3]
        result.y = this[1, 3]
        result.z = this[2, 3]
        return result
    }

    fun getRotation(result: Mat3): Mat3 {
        result[0, 0] = this[0, 0]
        result[0, 1] = this[0, 1]
        result[0, 2] = this[0, 2]

        result[1, 0] = this[1, 0]
        result[1, 1] = this[1, 1]
        result[1, 2] = this[1, 2]

        result[2, 0] = this[2, 0]
        result[2, 1] = this[2, 1]
        result[2, 2] = this[2, 2]

        return result
    }

    fun getRotationTransposed(result: Mat3): Mat3 {
        result[0, 0] = this[0, 0]
        result[0, 1] = this[1, 0]
        result[0, 2] = this[2, 0]

        result[1, 0] = this[0, 1]
        result[1, 1] = this[1, 1]
        result[1, 2] = this[2, 1]

        result[2, 0] = this[0, 2]
        result[2, 1] = this[1, 2]
        result[2, 2] = this[2, 2]

        return result
    }

    fun getRotation(result: MutableVec4f): MutableVec4f {
        val trace = this[0, 0] + this[1, 1] + this[2, 2]

        if (trace > 0f) {
            var s = sqrt(trace + 1f)
            result.w = s * 0.5f
            s = 0.5f / s

            result.x = (this[2, 1] - this[1, 2]) * s
            result.y = (this[0, 2] - this[2, 0]) * s
            result.z = (this[1, 0] - this[0, 1]) * s
        } else {
            val i =
                if (this[0, 0] < this[1, 1]) {
                    if (this[1, 1] < this[2, 2]) {
                        2
                    } else {
                        1
                    }
                } else {
                    if (this[0, 0] < this[2, 2]) {
                        2
                    } else {
                        0
                    }
                }
            val j = (i + 1) % 3
            val k = (i + 2) % 3

            var s = sqrt(this[i, i] - this[j, j] - this[k, k] + 1f)
            result[i] = s * 0.5f
            s = 0.5f / s

            result.w = (this[k, j] - this[j, k]) * s
            result[j] = (this[j, i] + this[i, j]) * s
            result[k] = (this[k, i] + this[i, k]) * s
        }
        return result
    }

    fun toBuffer(buffer: FloatBuffer): FloatBuffer {
        buffer.put(data, 0, 16)
        buffer.flip()
        return buffer
    }

    fun toList(): List<Float> {
        val list = mutableListOf<Float>()
        for (i in 0..15) {
            list += data[i]
        }
        return list
    }

    fun dump() {
        for (r in 0..3) {
            for (c in 0..3) {
                print("${this[r, c]} ")
            }
            println()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Mat4

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    companion object {
        private val tmpMatLock = Any()
        private val tmpMatA = Mat4()
        private val tmpMatB = Mat4()
        private val l_vez = MutableVec3f()
        private val l_vex = MutableVec3f()
        private val l_vey = MutableVec3f()
        private val right = MutableVec3f()
        private val tmpForward = MutableVec3f()
        private val tmpUp = MutableVec3f()
        private val tmpVec = MutableVec3f()
    }
}
