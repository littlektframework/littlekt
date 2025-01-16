package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
class AmbientLight(val color: Color = DEFAULT_COLOR) : Light {
    companion object {
        val DEFAULT_COLOR = Color(0.1f, 0.1f, 0.1f)
    }
}
