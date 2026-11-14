package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.particle.*
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of GPU particles. Each [ParticleEmitter] is simulated with a compute pass and drawn
 * with a [ParticleRenderer]: fire, smoke, a spark fountain, a ground glow with a ring of sparks, a trail that follows a
 * moving emitter, and explosions. Press space for an explosion.
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
class GpuParticlesExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(1f, 0.95f, 0.85f),
                        direction = MutableVec3f(0.4f, 1f, 0.3f).norm(),
                        intensity = 1.5f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.08f, 0.08f, 0.1f)))
            }
        val floorMesh = fullIndexedMesh().generate {
            grid {
                sizeX = 40f
                sizeY = 40f
            }
        }
        val floorMaterial =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                baseColorTexture = Textures.textureWhite,
                baseColorFactor = Color(0.18f, 0.19f, 0.22f, 1f),
            )
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val particles =
            ParticleRenderer(device).apply {
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }

        val fire =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 220f,
                        lifetime = 0.6f..1.1f,
                        shape = EmitterShape.Circle(0.35f),
                        spread = 12.degrees,
                        speed = 1.2f..2.2f,
                        size = 0.5f..0.8f,
                        sizes = listOf(0.6f, 1f, 0.2f),
                        colors =
                            listOf(
                                Color(1f, 0.9f, 0.4f, 1f),
                                Color(1f, 0.45f, 0.1f, 0.8f),
                                Color(0.6f, 0.1f, 0.05f, 0f),
                            ),
                        angularVelocity = -90f..90f,
                        blend = ParticleBlend.ADDITIVE,
                    ),
                )
                .apply { position(-5f, 0f, 0f) }
        val smoke =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 25f,
                        lifetime = 2.5f..3.5f,
                        shape = EmitterShape.Circle(0.3f),
                        spread = 15.degrees,
                        speed = 0.8f..1.2f,
                        drag = 0.3f,
                        size = 0.8f..1.2f,
                        sizes = listOf(0.5f, 2.5f),
                        colors =
                            listOf(
                                Color(0.3f, 0.3f, 0.3f, 0f),
                                Color(0.25f, 0.25f, 0.25f, 0.5f),
                                Color(0.2f, 0.2f, 0.2f, 0f),
                            ),
                        angularVelocity = -30f..30f,
                    ),
                )
                .apply { position(-5f, 1.6f, 0f) }
        val fountain =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 4096,
                        rate = 600f,
                        lifetime = 1.2f..1.8f,
                        spread = 18.degrees,
                        speed = 7f..9f,
                        gravity = Vec3f(0f, -9.8f, 0f),
                        size = 0.04f..0.07f,
                        colors = listOf(Color(0.6f, 0.85f, 1f, 1f), Color(0.2f, 0.4f, 1f, 0f)),
                        alignment = ParticleAlignment.VELOCITY,
                        stretch = 0.6f,
                        blend = ParticleBlend.ADDITIVE,
                    ),
                )
                .apply { position(0f, 0f, 0f) }
        val glow =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 4f,
                        lifetime = 1.5f..1.5f,
                        speed = 0f..0f,
                        size = 1f..1f,
                        sizes = listOf(0.5f, 4f),
                        colors = listOf(Color(0.6f, 0.3f, 1f, 0.9f), Color(0.6f, 0.3f, 1f, 0f)),
                        rotation = 0f..0f,
                        alignment = ParticleAlignment.HORIZONTAL,
                        blend = ParticleBlend.ADDITIVE,
                    ),
                )
                .apply { position(5f, 0.02f, 0f) }
        val ring =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 150f,
                        lifetime = 0.8f..1.4f,
                        shape = EmitterShape.Circle(1.5f, edge = true),
                        spread = 5.degrees,
                        speed = 0.8f..1.6f,
                        size = 0.06f..0.12f,
                        colors = listOf(Color(0.8f, 0.6f, 1f, 1f), Color(0.4f, 0.2f, 1f, 0f)),
                        blend = ParticleBlend.ADDITIVE,
                    ),
                )
                .apply { position(5f, 0f, 0f) }
        val trail =
            ParticleEmitter(
                particles,
                ParticleEffect(
                    rate = 300f,
                    lifetime = 0.8f..1.2f,
                    spread = 180.degrees,
                    speed = 0.1f..0.3f,
                    size = 0.15f..0.25f,
                    sizes = listOf(1f, 0f),
                    colors = listOf(Color(0.3f, 1f, 0.6f, 1f), Color(0.1f, 0.5f, 1f, 0f)),
                    blend = ParticleBlend.ADDITIVE,
                ),
            )
        val explosion =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 4096,
                        rate = 0f,
                        lifetime = 0.6f..1.4f,
                        shape = EmitterShape.Sphere(0.3f),
                        spread = 180.degrees,
                        speed = 0f..1f,
                        radialSpeed = 4f..9f,
                        gravity = Vec3f(0f, -4f, 0f),
                        drag = 1.5f,
                        size = 0.1f..0.25f,
                        colors =
                            listOf(
                                Color(1f, 1f, 0.8f, 1f),
                                Color(1f, 0.5f, 0.1f, 1f),
                                Color(0.5f, 0.1f, 0.05f, 0f),
                            ),
                        alignment = ParticleAlignment.VELOCITY,
                        stretch = 0.15f,
                        blend = ParticleBlend.ADDITIVE,
                    ),
                )
                .apply { position(0f, 2f, -5f) }
        val emitters = listOf(fire, smoke, fountain, glow, ring, trail, explosion)

        val clearColor = graphics.surfaceColor(Color(0.05f, 0.05f, 0.07f, 1f))

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        var time = 0f
        var nextExplosion = 1f
        onUpdate { dt ->
            time += dt.seconds
            camera.position.set(sin(time * 0.1f) * 16f, 7f, cos(time * 0.1f) * 16f)
            camera.up.set(Vec3f.Y_AXIS)
            camera.lookAt(0f, 1.5f, 0f)
            camera.update()
            trail.position(cos(time * 2f) * 3f, 1.5f + sin(time * 3f) * 0.5f, 5f + sin(time * 2f) * 3f)
            if (input.isKeyJustPressed(Key.SPACE) || time >= nextExplosion) {
                explosion.burst(800)
                nextExplosion = time + 2.5f
            }
            floor.update(dt)
            emitters.forEach { it.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("Particles Command Encoder") { frame, commandEncoder ->
                particles.simulate(commandEncoder, emitters)
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Particles Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                modelBatch.flush(renderPass, camera, dt)
                particles.render(renderPass, camera, emitters)
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            emitters.forEach { it.release() }
            particles.release()
            modelBatch.release()
            environment.release()
            floorMesh.release()
            floorMaterial.release()
        }
    }
}
