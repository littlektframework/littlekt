package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color
import com.littlekt.math.Vec3f
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
class PointLight(
    val position: Vec3f,
    val color: Color = Color.WHITE,
    val intensity: Float = 1f,
    range: Float? = null,
) : Light {

    val range: Float =
        if (range != null && range >= 0) range
        else
            run {
                val radius = 0.05f
                val illuminationThreshold = 0.001f
                radius * (sqrt(intensity / illuminationThreshold) - 1)
            }
}
