package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.particle.*
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.renderFrame
import com.littlekt.resources.Fonts
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ExtendViewport
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * An example of 2D GPU particles drawn with a [SpriteBatch]: a campfire, sparks from a spinning
 * wheel, a comet trail and fireworks. Emitters are simulated before the render pass, and the batch
 * is flushed before particles are drawn so sprites keep their order. Press space for a firework.
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
class GpuParticles2DExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val icon = resourcesVfs["icon_32x32.png"].readTexture()
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera
        val text = TextureFontCache(Fonts.default)
        val clearColor = graphics.surfaceColor(Color(0.05f, 0.05f, 0.09f, 1f))

        val particles =
            ParticleRenderer(device).apply {
                colorFormat = graphics.preferredFormat
                depthFormat = null
            }
        val fire =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 8192,
                        rate = 900f,
                        lifetime = 0.6f..1.2f,
                        shape = EmitterShape.Box(Vec3f(30f, 4f, 0f)),
                        spread = 10.degrees,
                        speed = 80f..160f,
                        size = 18f..30f,
                        sizes = listOf(0.6f, 1f, 0.2f),
                        colors =
                            listOf(
                                Color(1f, 0.85f, 0.4f, 0.5f),
                                Color(1f, 0.35f, 0.1f, 0.4f),
                                Color(0.5f, 0.1f, 0.05f, 0f),
                            ),
                        angularVelocity = -90f..90f,
                        blend = ParticleBlend.ADDITIVE,
                        flat = true,
                    ),
                )
                .position(-250f, -150f)
        val sparks =
            ParticleEmitter(
                particles,
                ParticleEffect(
                    maxParticles = 4096,
                    rate = 600f,
                    lifetime = 0.5f..1f,
                    direction = Vec3f.X_AXIS,
                    spread = 8.degrees,
                    speed = 250f..400f,
                    gravity = Vec3f(0f, -500f, 0f),
                    size = 2f..3f,
                    colors = listOf(Color(1f, 0.95f, 0.6f, 1f), Color(1f, 0.4f, 0.1f, 0f)),
                    alignment = ParticleAlignment.VELOCITY,
                    stretch = 0.03f,
                    blend = ParticleBlend.ADDITIVE,
                    flat = true,
                ),
            )
        val comet =
            ParticleEmitter(
                particles,
                ParticleEffect(
                    maxParticles = 4096,
                    rate = 800f,
                    lifetime = 0.6f..1f,
                    spread = 180.degrees,
                    speed = 5f..20f,
                    size = 6f..12f,
                    sizes = listOf(1f, 0f),
                    colors = listOf(Color(0.4f, 1f, 0.8f, 1f), Color(0.2f, 0.4f, 1f, 0f)),
                    blend = ParticleBlend.ADDITIVE,
                    flat = true,
                ),
            )
        val fireworks =
            ParticleEmitter(
                particles,
                ParticleEffect(
                    maxParticles = 4096,
                    rate = 0f,
                    lifetime = 0.8f..1.6f,
                    shape = EmitterShape.Circle(2f),
                    spread = 180.degrees,
                    speed = 120f..260f,
                    gravity = Vec3f(0f, -120f, 0f),
                    drag = 1.2f,
                    size = 3f..5f,
                    colors = listOf(Color(1f, 0.6f, 1f, 1f), Color(0.6f, 0.3f, 1f, 0f)),
                    alignment = ParticleAlignment.VELOCITY,
                    stretch = 0.02f,
                    blend = ParticleBlend.ADDITIVE,
                    flat = true,
                ),
            )
        val emitters = listOf(fire, sparks, comet, fireworks)

        val wheelX = 150f
        val wheelY = -80f
        var wheelAngle = 0f
        var time = 0f
        var nextFirework = 0.5f

        onResize { width, height ->
            viewport.update(width, height)
            text.setText(
                "2D GPU particles: space for a firework",
                -viewport.virtualWidth * 0.5f + 16f,
                -viewport.virtualHeight * 0.5f + 16f,
            )
        }

        onUpdate { dt ->
            time += dt.seconds
            wheelAngle += dt.seconds * 2f
            sparks
                .position(wheelX + cos(wheelAngle) * 24f, wheelY + sin(wheelAngle) * 24f)
                .rotation(wheelAngle.radians)
            comet.position(cos(time * 1.3f) * 320f, 120f + sin(time * 2.1f) * 90f)
            if (input.isKeyJustPressed(Key.SPACE) || time >= nextFirework) {
                fireworks.position(Random.nextFloat() * 500f - 200f, Random.nextFloat() * 140f + 60f)
                fireworks.burst(600)
                nextFirework = time + 1.5f
            }
            emitters.forEach { it.update(dt) }

            renderFrame("gpu particles 2d") { frame, commandEncoder ->
                particles.simulate(commandEncoder, emitters)
                val pass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor), label = "2D pass")
                    )
                camera.update()
                batch.begin(camera.viewProjection)
                particles.render(pass, camera, fire)
                batch.draw(icon, -282f, -166f, width = 64f, height = 32f)
                batch.draw(
                    icon,
                    wheelX - 24f,
                    wheelY - 24f,
                    originX = 24f,
                    originY = 24f,
                    width = 48f,
                    height = 48f,
                    rotation = wheelAngle.radians,
                )
                batch.flush(pass)
                particles.render(pass, camera, sparks)
                particles.render(pass, camera, comet)
                particles.render(pass, camera, fireworks)
                text.draw(batch)
                batch.flush(pass)
                batch.end()
                pass.end()
                pass.release()
            }
        }

        onRelease {
            emitters.forEach { it.release() }
            particles.release()
            batch.release()
            icon.release()
        }
    }
}
