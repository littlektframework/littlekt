package com.littlekt.graphics.g3d.light

import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees

/**
 * A light that shines from [position] towards [direction] in a cone, such as a flashlight. Surfaces
 * within [innerAngle] of the direction get the full light which smoothly fades out to nothing at
 * [outerAngle]. Every field can be changed in place each frame.
 *
 * ```
 * val flashlight = SpotLight(intensity = 20f, range = 15f)
 * environment.addSpotLight(flashlight)
 * // each frame
 * flashlight.position.set(camera.position)
 * flashlight.direction.set(camera.direction)
 * ```
 *
 * @param position the initial position of the light
 * @param direction the initial direction the light points to
 * @param color the initial color of the light
 * @param intensity the strength of the light
 * @param range how far the light reaches
 * @param innerAngle the angle from [direction] that gets the full light
 * @param outerAngle the angle from [direction] where the light ends
 * @param castShadows if the light casts shadows into a
 *   [com.littlekt.graphics.g3d.shadow.SpotShadowMap]
 * @author Colton Daily
 * @date 11/27/2026
 */
class SpotLight(
    position: Vec3f = Vec3f.ZERO,
    direction: Vec3f = Vec3f.NEG_Z_AXIS,
    color: Color = Color.WHITE,
    var intensity: Float = 1f,
    var range: Float = 10f,
    var innerAngle: Angle = 20.degrees,
    var outerAngle: Angle = 30.degrees,
    var castShadows: Boolean = false,
) : Light {
    /** The position of the light. */
    val position: MutableVec3f = MutableVec3f(position)

    /** The direction the light points to. Doesn't need to be normalized. */
    val direction: MutableVec3f = MutableVec3f(direction)

    /** The color of the light. */
    val color: MutableColor = MutableColor(color.r, color.g, color.b, color.a)
}
