package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.plus
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.internal.now
import com.lehaine.littlekt.util.seconds
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
class ParticleSimulator(maxParticles: Int) {

    val particles = List(maxParticles) { init(Particle(Textures.white).apply { index = it }) }

    private var numAlloc = 0

    fun alloc(slice: TextureSlice, x: Float, y: Float): Particle {
        return if (numAlloc < particles.size - 1) {
            val particle = reset(particles[numAlloc], slice).also {
                it.x = x
                it.y = y
            }
            numAlloc++
            particle
        } else {
            var best: Particle? = null
            particles.fastForEach {
                val b = best
                if (b == null || it.timeStamp < b.timeStamp) {
                    best = it
                }
            }
            best?.onKill?.invoke()
            best?.let {
                reset(it, slice)
                it.x = x
                it.y = y
            }
            best!!
        }
    }

    private fun init(particle: Particle): Particle {
        with(particle) {
            scale(1f)
            rotation = Angle.ZERO
            color.set(Color.WHITE)
            visible = true
            alpha = 1f

            data0 = 0
            data1 = 0
            data2 = 0
            data3 = 0

            xDelta = 0f
            yDelta = 0f
            scaleDelta = 0f
            scaleDeltaX = 0f
            scaleDeltaY = 0f
            scaleFriction = 1f
            scaleMultiplier = 1f
            scaleXMultiplier = 1f
            scaleYMultiplier = 1f
            rotationDelta = 0f
            rotationFriction = 1f
            frictionX = 1f
            frictionY = 1f
            gravityX = 0f
            gravityY = 0f
            fadeOutSpeed = 0.1f
            life = 1.seconds

            colorRdelta = 0f
            colorGdelta = 0f
            colorBdelta = 0f
            alphaDelta = 0f

            onStart = null
            onUpdate = null
            onKill = null

            timeStamp = now()
            killed = false
        }
        return particle
    }

    private fun reset(particle: Particle, slice: TextureSlice): Particle {
        val result = init(particle)
        result.slice = slice

        return result
    }

    private fun kill(particle: Particle) {
        particle.alpha = 0f
        particle.life = Duration.ZERO
        particle.killed = true
        particle.visible = false
    }

    private fun advance(particle: Particle, dt: Duration, tmod: Float) {
        particle.delay -= dt
        if (particle.killed || particle.delay > 0.milliseconds) return

        particle.onStart?.invoke()
        particle.onStart = null

        with(particle) {
            // gravity
            xDelta += gravityX * dt.seconds
            yDelta += gravityY * tmod

            // movement
            x += xDelta * tmod
            y += yDelta * tmod

            // friction
            if (frictionX == frictionY) {
                val frictTmod = frictionX.fastPow(tmod)
                xDelta *= frictTmod
                yDelta *= frictTmod
            } else {
                xDelta *= frictionX.fastPow(tmod)
                yDelta *= frictionY.fastPow(tmod)
            }

            // rotation
            rotation += (rotationDelta * tmod).radians
            rotationDelta *= rotationFriction * tmod

            // scale
            scaleX += (scaleDelta + scaleDeltaX) * tmod
            scaleY += (scaleDelta + scaleDeltaY) * tmod
            val scaleMul = scaleMultiplier.fastPow(tmod)
            scaleX *= scaleMul
            scaleX *= scaleXMultiplier.fastPow(tmod)
            scaleY *= scaleMul
            scaleY *= scaleYMultiplier.fastPow(tmod)
            val scaleFrictPow = scaleFriction.fastPow(tmod)
            scaleDelta *= scaleFrictPow
            scaleDeltaX *= scaleFrictPow
            scaleDeltaY *= scaleFrictPow

            // color
            val colorR = color.r + particle.colorRdelta * tmod
            val colorG = color.g + particle.colorGdelta * tmod
            val colorB = color.b + particle.colorBdelta * tmod
            val colorA = color.a + particle.alphaDelta * tmod
            color.set(colorR, colorG, colorB, colorA)

            // life
            remainingLife -= dt
            if (remainingLife <= 0.milliseconds) {
                alpha -= (fadeOutSpeed * tmod).toFloat()
            }

            if (remainingLife <= 0.milliseconds && alpha <= 0) {
                onKill?.invoke()
                kill(particle)
            } else {
                onUpdate?.invoke(particle)
            }
        }
    }

    fun simulate(dt: Duration, optionalTmod: Float = -1f) {
        val tmod = if (optionalTmod < 0) {
            dt.seconds * 60
        } else {
            optionalTmod
        }
        for (i in 0..numAlloc) {
            val particle = particles[i]
            advance(particle, dt, tmod)
        }
    }
}

private fun Float.fastPow(power: Float): Float {
    if (power == 1f || this == 0f || this == 1f) {
        return this
    }
    return pow(power)
}