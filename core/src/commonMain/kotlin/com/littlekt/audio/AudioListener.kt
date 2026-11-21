package com.littlekt.audio

import com.littlekt.graphics.Camera
import com.littlekt.math.Vec3f

/**
 * The point that sounds played with [AudioClip.playAt] are heard from, usually moved with the
 * camera or the player each frame. Sounds played with [AudioClip.play] aren't affected by it.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
interface AudioListener {
    /** Moves the listener to [x], [y], [z] in world space. */
    fun setPosition(x: Float, y: Float, z: Float)

    /**
     * Turns the listener to face the forward direction with the given up direction. Both should be
     * normalized.
     */
    fun setOrientation(
        forwardX: Float,
        forwardY: Float,
        forwardZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
    )
}

/** Moves the listener to [position] in world space. */
fun AudioListener.setPosition(position: Vec3f) = setPosition(position.x, position.y, position.z)

/** Turns the listener to face [forward] with [up] as its up direction. */
fun AudioListener.setOrientation(forward: Vec3f, up: Vec3f) =
    setOrientation(forward.x, forward.y, forward.z, up.x, up.y, up.z)

/** Moves and turns the listener to match [camera]. */
fun AudioListener.set(camera: Camera) {
    setPosition(camera.position)
    setOrientation(camera.direction, camera.up)
}
