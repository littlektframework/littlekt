package com.littlekt.graphics.particle

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees

/**
 * How a [ParticleEmitter] spawns, moves and draws its particles. Ranges pick a random value for
 * each particle. Sizes & speeds are in world units, times in seconds.
 *
 * ```
 * val sparks = ParticleEffect(
 *     rate = 200f,
 *     lifetime = 0.4f..0.9f,
 *     speed = 4f..8f,
 *     spread = 25.degrees,
 *     gravity = Vec3f(0f, -9.8f, 0f),
 *     colors = listOf(Color.YELLOW, Color(1f, 0.3f, 0f, 0f)),
 *     blend = ParticleBlend.ADDITIVE,
 * )
 * ```
 *
 * @property maxParticles the most particles alive at once. Once full, new particles replace the
 *   oldest.
 * @property rate particles spawned per second while [ParticleEmitter.emitting]
 * @property lifetime how long each particle lives
 * @property shape where particles spawn, relative to the emitter
 * @property direction the local direction particles move in
 * @property spread the angle around [direction] particles move within. `180` degrees moves in
 *   every direction.
 * @property speed the speed along the picked direction
 * @property radialSpeed the speed away from the emitter's center, added to [speed]
 * @property gravity the acceleration applied to each particle
 * @property drag how quickly particles slow down, as a fraction of their speed per second
 * @property inheritVelocity how much of the emitter's movement new particles keep
 * @property size the size of each particle
 * @property sizes the size multiplier over each particle's life, spread evenly from birth to
 *   death. Up to [MAX_KEYS] values.
 * @property colors the color over each particle's life, spread evenly from birth to death. Up to
 *   [MAX_KEYS] values. Multiplied with [texture].
 * @property rotation the starting rotation of each particle
 * @property angularVelocity the rotation speed of each particle, in degrees per second
 * @property alignment how each particle faces
 * @property stretch how much [ParticleAlignment.VELOCITY] particles grow along their velocity, per
 *   unit of speed
 * @property blend how particles are blended with what is behind them
 * @property localSpace `true` to keep particles relative to the emitter so they move with it,
 *   `false` to leave them in the world where they spawned
 * @property texture the texture of each particle, or `null` for a soft round dot
 * @property flat `true` to keep particles on the emitter's XY plane, such as for 2D. Directions,
 *   shapes & gravity ignore `z`, and [EmitterShape.Sphere] & [EmitterShape.Circle] spawn on a disc.
 * @author Colton Daily
 * @date 11/14/2026
 */
data class ParticleEffect(
    val maxParticles: Int = 2048,
    val rate: Float = 50f,
    val lifetime: ClosedFloatingPointRange<Float> = 1f..1.5f,
    val shape: EmitterShape = EmitterShape.Point,
    val direction: Vec3f = Vec3f.Y_AXIS,
    val spread: Angle = 20.degrees,
    val speed: ClosedFloatingPointRange<Float> = 1f..2f,
    val radialSpeed: ClosedFloatingPointRange<Float> = 0f..0f,
    val gravity: Vec3f = Vec3f.ZERO,
    val drag: Float = 0f,
    val inheritVelocity: Float = 0f,
    val size: ClosedFloatingPointRange<Float> = 0.2f..0.3f,
    val sizes: List<Float> = listOf(1f),
    val colors: List<Color> = listOf(Color.WHITE, Color(1f, 1f, 1f, 0f)),
    val rotation: ClosedFloatingPointRange<Float> = 0f..360f,
    val angularVelocity: ClosedFloatingPointRange<Float> = 0f..0f,
    val alignment: ParticleAlignment = ParticleAlignment.BILLBOARD,
    val stretch: Float = 0.1f,
    val blend: ParticleBlend = ParticleBlend.ALPHA,
    val localSpace: Boolean = false,
    val texture: Texture? = null,
    val flat: Boolean = false,
) {
    init {
        require(maxParticles > 0) { "maxParticles must be greater than 0!" }
        require(rate >= 0f) { "rate can't be negative!" }
        require(lifetime.start > 0f) { "lifetime must be greater than 0!" }
        require(sizes.size in 1..MAX_KEYS) { "sizes must have 1 to $MAX_KEYS values!" }
        require(colors.size in 1..MAX_KEYS) { "colors must have 1 to $MAX_KEYS values!" }
    }

    companion object {
        /** The most [sizes] & [colors] keys. */
        const val MAX_KEYS = 4
    }
}

/**
 * Where a [ParticleEmitter] spawns particles, in its local space.
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
sealed class EmitterShape {
    /** Spawns at the emitter's position. */
    data object Point : EmitterShape()

    /**
     * Spawns inside a sphere, or on its surface if [surface] is `true`.
     *
     * @property radius the sphere radius
     */
    data class Sphere(val radius: Float, val surface: Boolean = false) : EmitterShape()

    /**
     * Spawns inside a box.
     *
     * @property halfExtents half the size of the box on each axis
     */
    data class Box(val halfExtents: Vec3f) : EmitterShape()

    /**
     * Spawns on a flat circle on the local XZ plane, or on its edge if [edge] is `true`.
     *
     * @property radius the circle radius
     */
    data class Circle(val radius: Float, val edge: Boolean = false) : EmitterShape()
}

/**
 * How particles face.
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
enum class ParticleAlignment {
    /** Always faces the camera. */
    BILLBOARD,

    /** Stretches along its velocity while facing the camera, such as for sparks & rain. */
    VELOCITY,

    /** Lies flat on the world XZ plane, such as for ground rings & ripples. */
    HORIZONTAL,
}

/**
 * How particles are blended with what is behind them.
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
enum class ParticleBlend {
    /** Standard transparency. Particles aren't sorted, so overlapping colors can mix in any order. */
    ALPHA,

    /** Adds light, such as for fire, sparks & magic. Order doesn't matter. */
    ADDITIVE,
}
