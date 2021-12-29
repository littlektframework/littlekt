package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.milliseconds
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
class Particle(var slice: TextureSlice) {
    var x: Float = 0f
    var y: Float = 0f
    var anchorX: Float = 0f
    var anchorY: Float = 0f
    var scaleX: Float = 0f
    var scaleY: Float = 0f
    var rotation: Angle = Angle.ZERO
    var color = Color.WHITE.toMutableColor() // TODO - possible use a field delegate here so we can update the colorBits when using color.set()
    var colorBits = color.toFloatBits()
    var alpha: Float
        get() = color.a
        set(value) {
            color.a = value
        }

    var visible: Boolean = true

    var index: Int = 0
    var xDelta: Float = 0f
    var yDelta: Float = 0f

    var scaleDelta: Float = 0f
    var scaleDeltaX: Float = 0f
    var scaleDeltaY: Float = 0f
    var scaleFriction: Float = 1f
    var scaleMultiplier: Float = 1f
    var scaleXMultiplier: Float = 1f
    var scaleYMultiplier: Float = 1f
    var rotationDelta: Float = 0f
    var rotationFriction: Float = 1f
    var friction: Float
        get() = (frictionX + frictionY) * 0.5f
        set(value) {
            frictionX = value
            frictionY = value
        }

    var frictionX: Float = 1f
    var frictionY: Float = 1f
    var gravityX: Float = 0f
    var gravityY: Float = 0f

    /**
     * The speed to fade out the particle after [remainingLife] is 0
     */
    var fadeOutSpeed: Float = 0.1f

    /**
     * Total particle life
     */
    var life: Duration = 1.seconds
        set(value) {
            field = value
            remainingLife = value
        }

    /**
     * Life remaining before being killed
     */
    var remainingLife: Duration = ZERO

    /**
     * Time to delay the particle from starting updates
     */
    var delay: Duration = ZERO

    var killed = false

    val alive get() = remainingLife.milliseconds > 0

    var onStart: (() -> Unit)? = null
    var onUpdate: ((Particle) -> Unit)? = null
    var onKill: (() -> Unit)? = null

    var colorRdelta: Float = 0f
    var colorGdelta: Float = 0f
    var colorBdelta: Float = 0f
    var alphaDelta: Float = 0f

    var timeStamp: Double = 0.0

    var data0 = 0
    var data1 = 0
    var data2 = 0
    var data3 = 0

    fun moveAwayFrom(x: Float, y: Float, speed: Float) {
        val angle = atan2(y - this.y, x - this.x)
        xDelta = -cos(angle) * speed
        yDelta = -sin(angle) * speed
    }

    fun scale(value: Float) {
        scaleX = value
        scaleY = value
    }

    override fun toString(): String {
        return "Particle(index=$index, xDelta=$xDelta, yDelta=$yDelta, scaleDelta=$scaleDelta, scaleDeltaX=$scaleDeltaX, scaleDeltaY=$scaleDeltaY, scaleFriction=$scaleFriction, scaleMultiplier=$scaleMultiplier, scaleXMultiplier=$scaleXMultiplier, scaleYMultiplier=$scaleYMultiplier, rotationDelta=$rotationDelta, rotationFriction=$rotationFriction, frictionX=$frictionX, frictionY=$frictionY, gravityX=$gravityX, gravityY=$gravityY, fadeOutSpeed=$fadeOutSpeed, life=$life, remainingLife=$remainingLife, delay=$delay, killed=$killed, onStart=$onStart, onUpdate=$onUpdate, onKill=$onKill, colorRdelta=$colorRdelta, colorGdelta=$colorGdelta, colorBdelta=$colorBdelta, alphaDelta=$alphaDelta, timeStamp=$timeStamp, data0=$data0, data1=$data1, data2=$data2, data3=$data3)"
    }
}


fun Particle.draw(batch: SpriteBatch) {
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
        colorBits = colorBits
    )
}