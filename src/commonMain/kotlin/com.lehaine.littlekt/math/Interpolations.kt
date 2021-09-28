package com.lehaine.littlekt.math

import com.lehaine.littlekt.Percent
import com.lehaine.littlekt.Seconds
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sin

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
object Interpolations {

    fun lerp(target: Float, current: Float, step: Float = 0.9f): Float {
        return target + step * (current - target)
    }

    fun lerp(target: Float, current: Float, step: Float = 0.9f, deltaTime: Seconds): Float {
        return lerp(target, current, 1 - step.pow(deltaTime))
    }

    fun lerp(target: Mat4, current: Mat4, step: Float = 0.9f): Mat4 {
        val translationTarget = target.translation
        val rotationTarget = Quaternion.from(target)
        val scaleTarget = target.scale

        val translationCurrent = current.translation
        val rotationCurrent = Quaternion.from(current)
        val scaleCurrent = current.scale

        val t = translation(
            Float3(
                lerp(translationTarget.x, translationCurrent.x, step),
                lerp(translationTarget.y, translationCurrent.y, step),
                lerp(translationTarget.z, translationCurrent.z, step)
            )
        )
        val r = interpolate(rotationCurrent, rotationTarget, 0.5f)
        val s = scale(
            Float3(
                lerp(scaleTarget.x, scaleCurrent.x, step),
                lerp(scaleTarget.y, scaleCurrent.y, step),
                lerp(scaleTarget.z, scaleCurrent.z, step)
            )
        )

        return t * Mat4.from(r) * s
    }

    fun lerp(target: Mat4, current: Mat4, step: Float = 0.9f, deltaTime: Seconds): Mat4 {
        return lerp(target, current, 1 - step.pow(deltaTime))
    }

    fun interpolate(target: Mat4, start: Mat4, blend: Percent): Mat4 {
        val blenderAsFloat = blend.toFloat()
        val trans = interpolate(start.translation, target.translation, blenderAsFloat)
        val rot = slerp(Quaternion.from(start), Quaternion.from(target), blenderAsFloat)
        val scale = interpolate(start.scale, target.scale, blenderAsFloat)

        return translation(Float3(trans.x, trans.y, trans.z)) * Mat4.from(rot) * scale(scale)
    }

    fun interpolate(target: Float, start: Float, blend: Percent): Float {
        return start + (target - start) * blend.toFloat()
    }

    fun slerp(q1: Quaternion, q2: Quaternion, t: Float): Quaternion {
        // Create a local quaternion to store the interpolated quaternion
        if (q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w) {
            return q1
        }
        var result = (
                q1.x * q2.x + q1.y * q2.y + q1.z * q2.z +
                        q1.w * q2.w
                )
        val qq2 = if (result < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            result = -result
            Quaternion(-q2.x, -q2.y, -q2.z, -q2.w)
        } else {
            q2
        }

        // Set the first and second scale for the interpolation
        var scale0 = 1 - t
        var scale1 = t

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if (1 - result > 0.1f) { // Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            val theta: Float = acos(result)
            val invSinTheta: Float = 1f / sin(theta)

            // Calculate the scale for q1 and q2, according to the angle and
            // its sine
            scale0 = sin((1 - t) * theta) * invSinTheta
            scale1 = sin(t * theta) * invSinTheta
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        val x = scale0 * q1.x + scale1 * qq2.x
        val y = scale0 * q1.y + scale1 * qq2.y
        val z = scale0 * q1.z + scale1 * qq2.z
        val w = scale0 * q1.w + scale1 * qq2.w

        // Return the interpolated quaternion
        return Quaternion(x, y, z, w)
    }
}