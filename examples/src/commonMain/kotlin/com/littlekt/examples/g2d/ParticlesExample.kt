package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.examples.colorAttachment
import com.littlekt.examples.configureSurfaceOnResize
import com.littlekt.examples.renderFrame
import com.littlekt.examples.surfaceColor
import com.littlekt.file.vfs.readAtlas
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.Particle
import com.littlekt.graphics.g2d.ParticleSimulator
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.draw
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.BlendState
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.PI2_F
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.resources.Textures
import com.littlekt.util.viewport.ExtendViewport
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * An example of simulating particles with a [ParticleSimulator]. A campfire burns on its own while
 * clicking, or holding, the mouse sets off bursts of sparks that bounce off the ground.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class ParticlesExample(context: Context) : ContextListener(context) {
    private val fire = ParticleSimulator(2048)
    private val smoke = ParticleSimulator(512)
    private val groundY = 40f

    private val bounce: (Particle) -> Unit = { p ->
        if (p.y < groundY && p.yDelta < 0f) {
            p.y = groundY
            p.yDelta *= -0.45f
            p.xDelta *= 0.7f
        }
    }

    private fun rand(min: Float, max: Float) = min + Random.nextFloat() * (max - min)

    private fun Particle.setup(scale: Float, lifeSeconds: Float) {
        anchorX = 0.5f
        anchorY = 0.5f
        scale(scale)
        life = lifeSeconds.toDouble().seconds
        alphaDelta = -color.a / (lifeSeconds * 60f)
    }

    private fun Batch.drawLog(x: Float, rotation: Angle, color: Color) =
        draw(
            Textures.white,
            x - 14f,
            groundY,
            originX = 14f,
            originY = 2f,
            width = 28f,
            height = 4f,
            rotation = rotation,
            color = color,
        )

    private fun campfire(glow: TextureSlice, ember: TextureSlice, puff: TextureSlice, x: Float) {
        fire.alloc(glow, x + rand(-8f, 8f), groundY + rand(0f, 6f)).apply {
            color.set(1f, rand(0.55f, 0.8f), 0.2f, 0.8f)
            setup(rand(0.4f, 0.7f), rand(0.5f, 0.9f))
            yDelta = rand(0.6f, 1.2f)
            xDelta = rand(-0.15f, 0.15f)
            colorGdelta = -0.012f
            scaleMultiplier = 0.985f
        }
        if (Random.nextFloat() < 0.15f) {
            fire.alloc(ember, x + rand(-6f, 6f), groundY + 8f).apply {
                color.set(1f, rand(0.4f, 0.9f), 0.1f, 1f)
                setup(1f, rand(1f, 2f))
                yDelta = rand(0.8f, 1.6f)
                xDelta = rand(-0.4f, 0.4f)
                frictionX = 0.97f
                gravityY = -0.004f
            }
        }
        if (Random.nextFloat() < 0.08f) {
            smoke.alloc(puff, x + rand(-4f, 4f), groundY + 40f).apply {
                color.set(0.35f, 0.33f, 0.38f, 0.35f)
                setup(rand(0.2f, 0.35f), rand(2f, 3f))
                yDelta = rand(0.3f, 0.5f)
                xDelta = rand(0.05f, 0.25f)
                scaleDelta = 0.004f
                rotationDelta = rand(-0.02f, 0.02f)
            }
        }
    }

    private fun burst(spark: TextureSlice, glow: TextureSlice, x: Float, y: Float, count: Int) {
        fire.alloc(glow, x, y).apply {
            color.set(1f, 0.9f, 0.6f, 1f)
            setup(1.5f, 0.2f)
            scaleMultiplier = 1.05f
        }
        repeat(count) {
            val angle = rand(0f, PI2_F)
            val speed = rand(1.5f, 5f)
            fire.alloc(spark, x, y).apply {
                color.set(1f, rand(0.3f, 1f), rand(0f, 0.4f), 1f)
                setup(rand(0.5f, 1f), rand(1f, 2.5f))
                xDelta = cos(angle) * speed
                yDelta = sin(angle) * speed
                gravityY = -0.12f
                frictionX = 0.98f
                frictionY = 0.98f
                onUpdate = bounce
            }
        }
    }

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val glow = atlas.getByPrefix("fxGlow").slice
        val ember = atlas.getByPrefix("fxDot").slice
        val puff = atlas.getByPrefix("fxSmoke").slice
        val spark = atlas.getByPrefix("fxSmallCircle").slice
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat, size = 4000)
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.fromHex("141226"))
        val groundColor = Color.fromHex("2a2238")
        val logColor = Color.fromHex("5c3a21")
        val logAngle = 20f.degrees
        val cursor = MutableVec2f()
        val emitRate = 16.milliseconds
        var emitTimer = Duration.ZERO
        var sprayTimer = Duration.ZERO

        onResize { width, height -> viewport.update(width, height, true) }

        onUpdate { dt ->
            camera.update()
            val fireX = viewport.virtualWidth * 0.5f
            emitTimer += dt
            while (emitTimer >= emitRate) {
                campfire(glow, ember, puff, fireX)
                emitTimer -= emitRate
            }

            camera.screenToWorld(this, input.x.toFloat(), input.y.toFloat(), viewport, cursor)
            if (input.isJustTouched(Pointer.POINTER1)) {
                burst(spark, glow, cursor.x, cursor.y, 60)
                sprayTimer = Duration.ZERO
            } else if (input.isTouching(Pointer.POINTER1)) {
                sprayTimer += dt
                if (sprayTimer >= 60.milliseconds) {
                    burst(spark, glow, cursor.x, cursor.y, 12)
                    sprayTimer = Duration.ZERO
                }
            }

            fire.update(dt)
            smoke.update(dt)

            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, clearColor)))
                    )
                batch.use(renderPass, camera.viewProjection) {
                    it.draw(
                        Textures.white,
                        0f,
                        0f,
                        width = viewport.virtualWidth,
                        height = groundY,
                        color = groundColor,
                    )
                    it.drawLog(fireX, logAngle, logColor)
                    it.drawLog(fireX, -logAngle, logColor)
                    smoke.draw(it)
                    it.setBlendState(BlendState.Add)
                    fire.draw(it)
                    it.swapToPreviousBlendState()
                }
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            atlas.release()
        }
    }
}
