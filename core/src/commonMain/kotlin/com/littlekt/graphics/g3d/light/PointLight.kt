package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color
import com.littlekt.math.Vec3f
import kotlin.math.sqrt

/**
 * A light that shines from [position] in every direction up to [range]. Pass a
 * [com.littlekt.math.MutableVec3f] or [com.littlekt.graphics.MutableColor] to change them in place
 * each frame.
 *
 * @param range how far the light reaches. Defaults to a range based off the initial [intensity].
 * @author Colton Daily
 * @date 1/15/2025
 */
class PointLight(
    var position: Vec3f,
    var color: Color = Color.WHITE,
    var intensity: Float = 1f,
    range: Float? = null,
) : Light {

    var range: Float =
        range
            ?: run {
                val radius = 0.05f
                val illuminationThreshold = 0.001f
                radius * (sqrt(intensity / illuminationThreshold) - 1)
            }
}
