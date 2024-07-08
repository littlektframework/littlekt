package com.littlekt.graphics.g2d

import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.math.geom.Angle
import com.littlekt.util.milliseconds
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * A CPU-Based particle. Used in conjunction with [ParticleSimulator].
 *
 * @author Colton Daily
 * @date 12/29/2021
 */
class Particle(var slice: TextureSlice) {
    /** The current x-position. */
    var x: Float = 0f

    /** The current y-position. */
    var y: Float = 0f

    /**
     * Value from `0f` to `1f` for the x-anchor of the sprite, used in rendering. `0f, 0f` is
     * top-left while `1f, 1f` is bottom-right.
     */
    var anchorX: Float = 0f
    /**
     * Value from `0f` to `1f` for the y-anchor of the sprite, used in rendering. `0f, 0f` is
     * top-left while `1f, 1f` is bottom-right.
     */
    var anchorY: Float = 0f

    /** The current x-scale. */
    var scaleX: Float = 0f

    /** The current y-scale. */
    var scaleY: Float = 0f

    /** The current rotation. */
    var rotation: Angle = Angle.ZERO

    /** The current color. */
    val color: MutableColor = Color.WHITE.toMutableColor()

    /** The current alpha value of [color]. */
    var alpha: Float
        get() = color.a
        set(value) {
            color.a = value
        }

    /** @return `true` if visible for rendering */
    var visible: Boolean = true

    /** The index of the particle. */
    var index: Int = 0

    /** The x-delta to be used when calculating the new [x] value. */
    var xDelta: Float = 0f
    /** The y-delta to be used when calculating the new [y] value. */
    var yDelta: Float = 0f

    /** The scale delta to be used when calculating the new [scaleX] and [scaleY] values. */
    var scaleDelta: Float = 0f
    /** The x-scale delta to be used when calculating the new [scaleX] value. */
    var scaleDeltaX: Float = 0f
    /** The y-scale delta to be used when calculating the new [scaleY] value. */
    var scaleDeltaY: Float = 0f
    /** The scale friction to be used when calculating the new [scaleX] and [scaleY] values. */
    var scaleFriction: Float = 1f
    /** The scale multiplier to be used when calculating the new [scaleX] and [scaleY] values. */
    var scaleMultiplier: Float = 1f
    /** The x-scale multiplier to be used when calculating the new [scaleX] value. */
    var scaleXMultiplier: Float = 1f
    /** The y-scale multiplier to be used when calculating the new [scaleY] value. */
    var scaleYMultiplier: Float = 1f

    /** The rotation delta to be used when calculating the new [rotation] value. */
    var rotationDelta: Float = 0f
    /** The rotation friction to be used when calculating the new [rotation] value. */
    var rotationFriction: Float = 1f

    /**
     * The position friction to be used when calculating the new [x] and [y] values. Setting this
     * sets [frictionX] and [frictionY]
     */
    var friction: Float
        get() = (frictionX + frictionY) * 0.5f
        set(value) {
            frictionX = value
            frictionY = value
        }

    /** The x-position friction to be used when calculating the new [x] value. */
    var frictionX: Float = 1f
    /** The y-position friction to be used when calculating the new [y] value. */
    var frictionY: Float = 1f
    /** The strength of gravity in the x-axis to be used when calculating the new [x] value. */
    var gravityX: Float = 0f
    /** The strength of gravity in they-axis to be used when calculating the new [y] value. */
    var gravityY: Float = 0f

    /** The speed to fade out the particle after [remainingLife] is 0 */
    var fadeOutSpeed: Float = 0.1f

    /** Total particle life */
    var life: Duration = 1.seconds
        set(value) {
            field = value
            remainingLife = value
        }

    /** Life remaining before being killed */
    var remainingLife: Duration = ZERO

    /** Time to delay the particle from starting updates */
    var delay: Duration = ZERO

    /** @return `true` if this particle has been killed. */
    var killed = false

    /** @return `true` if this particle is still alive. */
    val alive: Boolean
        get() = remainingLife.milliseconds > 0

    /** Invoked when the particle is first initialized. */
    var onStart: (() -> Unit)? = null

    /** Invoked each frame that the particle is alive. */
    var onUpdate: ((Particle) -> Unit)? = null

    /** Invoked when the particle is killed. */
    var onKill: (() -> Unit)? = null

    /** The color delta to be used when calculating the new [color] red channel. */
    var colorRdelta: Float = 0f
    /** The color delta to be used when calculating the new [color] green channel. */
    var colorGdelta: Float = 0f
    /** The color delta to be used when calculating the new [color] blue channel. */
    var colorBdelta: Float = 0f
    /** The color delta to be used when calculating the new [color] alpha channel. */
    var alphaDelta: Float = 0f

    /** The timestamp, in milliseconds, when this particle was initialized. */
    var timeStamp: Double = 0.0

    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data0 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data0 = 1f
     *    }
     * ```
     */
    var data0 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data1 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data1 = 1f
     *    }
     * ```
     */
    var data1 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data2 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data2 = 1f
     *    }
     * ```
     */
    var data2 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data3 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data3 = 1f
     *    }
     * ```
     */
    var data3 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data4 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data4 = 1f
     *    }
     * ```
     */
    var data4 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data5 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data5 = 1f
     *    }
     * ```
     */
    var data5 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data6 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data6 = 1f
     *    }
     * ```
     */
    var data6 = 0f
    /**
     * A user data field that can be used in the event calls ([onStart], [onUpdate], [onKill]) to
     * mark or denote that something has happened to this particle. This will get reset to `0f` each
     * time the particle is reallocated.
     *
     * E.g:
     * ```
     * onUpdate { particle ->
     *    if(particle.data7 == 1f) {
     *       // do something
     *    } else if(particle.x >= 50f) {
     *      particle.data7 = 1f
     *    }
     * ```
     */
    var data7 = 0f

    /**
     * Helper function to calculate the [xDelta] and [yDelta] values to move the particle away from
     * a specific point.
     *
     * @param x x-coordinate to move away from
     * @param y y-coordinate to move away from
     * @param speed speed of particle
     */
    fun moveAwayFrom(x: Float, y: Float, speed: Float) {
        val angle = atan2(y - this.y, x - this.x)
        xDelta = -cos(angle) * speed
        yDelta = -sin(angle) * speed
    }

    /** Sets the [scaleX] and [scaleY] to the given [value]. */
    fun scale(value: Float) {
        scaleX = value
        scaleY = value
    }

    override fun toString(): String {
        return "Particle(index=$index, xDelta=$xDelta, yDelta=$yDelta, scaleDelta=$scaleDelta, scaleDeltaX=$scaleDeltaX, scaleDeltaY=$scaleDeltaY, scaleFriction=$scaleFriction, scaleMultiplier=$scaleMultiplier, scaleXMultiplier=$scaleXMultiplier, scaleYMultiplier=$scaleYMultiplier, rotationDelta=$rotationDelta, rotationFriction=$rotationFriction, frictionX=$frictionX, frictionY=$frictionY, gravityX=$gravityX, gravityY=$gravityY, fadeOutSpeed=$fadeOutSpeed, life=$life, remainingLife=$remainingLife, delay=$delay, killed=$killed, onStart=$onStart, onUpdate=$onUpdate, onKill=$onKill, colorRdelta=$colorRdelta, colorGdelta=$colorGdelta, colorBdelta=$colorBdelta, alphaDelta=$alphaDelta, timeStamp=$timeStamp, data0=$data0, data1=$data1, data2=$data2, data3=$data3. data4=$data4, data5=$data5, data6=$data6, data7=$data7)"
    }
}

fun Particle.draw(batch: Batch) {
    if (!visible || !alive) return

    batch.draw(
        slice,
        x,
        y,
        anchorX * slice.width,
        anchorY * slice.height,
        scaleX = scaleX,
        scaleY = scaleY,
        rotation = rotation,
        color = color
    )
}
