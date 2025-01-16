package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color
import com.littlekt.math.Vec3f

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
class DirectionalLight(
    val color: Color = Color.WHITE,
    val direction: Vec3f = Vec3f.UP,
    val intensity: Float = 1f,
) : Light
