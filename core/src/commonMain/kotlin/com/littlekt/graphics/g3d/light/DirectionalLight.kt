package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color
import com.littlekt.math.Vec3f

/**
 * A light that shines from far away in a single direction, such as the sun. Pass a
 * [com.littlekt.math.MutableVec3f] or [com.littlekt.graphics.MutableColor] to change them in place
 * each frame.
 *
 * @author Colton Daily
 * @date 1/15/2025
 */
class DirectionalLight(
    var color: Color = Color.WHITE,
    var direction: Vec3f = Vec3f.UP,
    var intensity: Float = 1f,
) : Light
