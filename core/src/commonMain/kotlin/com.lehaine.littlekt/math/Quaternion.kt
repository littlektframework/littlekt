package com.lehaine.littlekt.math

import com.lehaine.littlekt.math.abs
import kotlin.math.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {

    fun isIdentity() = x == 0f && y == 0f && z == 0f && w == 1f

    fun toFloatArray(): FloatArray {
        return floatArrayOf(x, y, z, w)
    }

    /**
     * Transform a quaternion in Euler angles.
     * Angles are in degrees.
     */
    fun toEulerAngles(): Float3 {
        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Source_Code_2
        // roll (x-axis rotation)
        val sinrCosp = 2f * (w * x + y * z)
        val cosrCosp = 1f - 2f * (x * x + y * y)
        val roll = atan2(sinrCosp, cosrCosp)

        // pitch (y-axis rotation)
        val sinp = 2f * (w * y - z * x)
        val pitch = if (abs(sinp) >= 1f)
            PI / 2f * sign(sinp) // use 90 degrees if out of range
        else
            asin(sinp)

        // yaw (z-axis rotation)
        val sinyCosp = 2f * (w * z + x * y)
        val cosyCosp = 1f - 2f * (y * y + z * z)
        val yaw = atan2(sinyCosp, cosyCosp)

        return Float3(roll, pitch, yaw)
    }

    /** Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining
     */
    fun mul(other: Quaternion): Quaternion {
        val newX: Float = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y
        val newY: Float = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z
        val newZ: Float = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x
        val newW: Float = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z
        return Quaternion(newX, newY, newZ, newW)
    }

    operator fun Quaternion.times(other: Quaternion): Quaternion = this.mul(other)

    override fun toString(): String {
        return "($x $y $z $w)"
    }

    companion object {

        fun identity(): Quaternion {
            return Quaternion(0f, 0f, 0f, 1f)
        }

        /**
         * Construct a quaternion using a [Mat4]
         */
        fun from(m2: Mat4): Quaternion {
            // https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
            // The code is a conversion from the `setFromAxis` method from LibGDX.
            val m = transpose(m2)
            val tr = m.x.x + m.y.y + m.z.z

            return if (tr > 0) {
                var s = sqrt(tr + 1f)
                val qw = s * 0.5f
                s = 0.5f / s
                val qx = (m.z.y - m.y.z) * s
                val qy = (m.x.z - m.z.x) * s
                val qz = (m.y.x - m.x.y) * s
                Quaternion(qx, qy, qz, qw)
            } else if ((m.x.x > m.y.y) && (m.x.x > m.z.z)) {
                var s = sqrt(1f + m.x.x - m.y.y - m.z.z)
                val qx = s * 0.5f
                s = 0.5f / s
                val qy = (m.y.x + m.x.y) * s
                val qz = (m.x.z + m.z.x) * s
                val qw = (m.z.y - m.y.z) * s
                Quaternion(qx, qy, qz, qw)
            } else if (m.y.y > m.z.z) {
                var s = sqrt(1.0f + m.y.y - m.x.x - m.z.z)
                val qy = s * 0.5f
                s = 0.5f / s
                val qx = (m.y.x + m.x.y) * s
                val qz = (m.y.z + m.z.y) * s
                val qw = (m.x.z - m.z.x) * s
                Quaternion(qx, qy, qz, qw)
            } else {
                var s = sqrt(1.0f + m.z.z - m.x.x - m.y.y)
                val qz = s * 0.5f
                s = 0.5f / s
                val qx = (m.x.z + m.z.x) * s
                val qy = (m.z.y + m.y.z) * s
                val qw = (m.y.x - m.x.y) * s
                Quaternion(qx, qy, qz, qw)
            }
        }

        /**
         * Create a Quaternion from Eulers angle.
         * Angles are expressed in degres.
         */
        fun fromEulers(x: Float, y: Float, z: Float, angle: Degrees): Quaternion {
            var d: Float = length(Float3(x, y, z))
            if (d == 0f) return identity()
            d = 1f / d
            val radians = radians(angle)
            val l_ang: Float = if (radians < 0) TWO_PI - -radians % TWO_PI else radians % TWO_PI
            val l_sin = sin(l_ang / 2f)
            return normalize(Quaternion(d * x * l_sin, d * y * l_sin, d * z * l_sin, cos(l_ang / 2f)))
        }

        /**
         * @see [fromEulers]
         */
        fun fromEulers(vector: Float3, angle: Degrees): Quaternion = fromEulers(vector.x, vector.y, vector.z, angle)
    }
}

fun normalize(quaternion: Quaternion): Quaternion {
    val (x, y, z, w) = quaternion
    val mag = sqrt(w * w + x * x + y * y + z * z)
    return Quaternion(x / mag, y / mag, z / mag, w / mag)
}

fun interpolate(a: Quaternion, b: Quaternion, blend: Float): Quaternion {
    val dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z
    val blendI = 1f - blend
    return if (dot < 0) {
        val w = blendI * a.w + blend * -b.w
        val x = blendI * a.x + blend * -b.x
        val y = blendI * a.y + blend * -b.y
        val z = blendI * a.z + blend * -b.z
        Quaternion(x, y, z, w)
    } else {
        val w = blendI * a.w + blend * b.w
        val x = blendI * a.x + blend * b.x
        val y = blendI * a.y + blend * b.y
        val z = blendI * a.z + blend * b.z
        Quaternion(x, y, z, w)
    }
}