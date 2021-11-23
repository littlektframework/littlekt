package com.lehaine.littlekt.math

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
data class Ray(var origin: Float3 = Float3(), var direction: Float3)

fun pointAt(r: Ray, t: Float) = r.origin + r.direction * t