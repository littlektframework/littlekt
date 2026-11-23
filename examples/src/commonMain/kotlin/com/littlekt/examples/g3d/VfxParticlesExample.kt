package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.ByteBuffer
import com.littlekt.graphics.Bloom
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.Tonemapper
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.particle.*
import com.littlekt.graphics.postprocess.DistortionEffect
import com.littlekt.graphics.postprocess.PostProcessor
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An example of particle effects for spells: [Flipbook] flames, [ParticleMesh] ice shards, rocks
 * & molten drops shaded with [ParticleRenderer.light], and [ParticleBlend.DISTORT] shockwaves &
 * heat haze drawn through a [DistortionEffect]. Press space for an ice nova.
 *
 * @author Colton Daily
 * @date 11/23/2026
 */
class VfxParticlesExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val hdrTarget =
            renderTarget(
                TextureFormat.RGBA16_FLOAT,
                usage = TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
            )
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        val sun = MutableVec3f(0.4f, 1f, 0.3f).norm()
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(DirectionalLight(color = Color(0.9f, 0.85f, 0.8f), direction = sun, intensity = 0.8f))
                setAmbientLight(AmbientLight(color = Color(0.12f, 0.12f, 0.15f)))
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
                baseColorFactor = Color(0.3f, 0.27f, 0.24f, 1f),
            )
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
            }
        val particles =
            ParticleRenderer(device).apply {
                colorFormat = hdrTarget.format
                depthFormat = depthTarget.format
                light.direction.set(-sun.x, -sun.y, -sun.z)
                light.color.set(1.2f, 1.15f, 1.1f, 1f)
                light.ambient.set(0.25f, 0.28f, 0.35f, 1f)
            }

        val distortion = DistortionEffect(device)
        val bloom = Bloom(device, hdrTarget.format)
        val tonemapper = Tonemapper(device, preferredFormat)
        val postProcessor = PostProcessor(device).apply { effects += listOf(distortion, tonemapper) }

        val flameSheet = createFlameSheet(device)
        val flames =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 60f,
                        lifetime = 0.7f..1.1f,
                        shape = EmitterShape.Circle(0.4f),
                        spread = 8.degrees,
                        speed = 0.6f..1.2f,
                        size = 0.9f..1.4f,
                        sizes = listOf(0.7f, 1f, 0.6f),
                        colors = listOf(Color(3f, 1.6f, 0.6f, 1f), Color(2f, 0.6f, 0.15f, 0.9f), Color(0.6f, 0.1f, 0.05f, 0f)),
                        rotation = -10f..10f,
                        blend = ParticleBlend.ADDITIVE,
                        texture = flameSheet,
                        flipbook = Flipbook(columns = 4, rows = 4),
                    ),
                )
                .apply { position(-2.5f, 0f, 0f) }
        val haze =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 12f,
                        lifetime = 1f..1.4f,
                        shape = EmitterShape.Circle(0.4f),
                        spread = 5.degrees,
                        speed = 1f..1.5f,
                        size = 1.5f..2f,
                        colors = listOf(Color(1f, 1f, 1f, 0f), Color(1f, 1f, 1f, 0.6f), Color(1f, 1f, 1f, 0f)),
                        angularVelocity = -40f..40f,
                        blend = ParticleBlend.DISTORT,
                        texture = distortion.hazeTexture,
                    ),
                )
                .apply { position(-2.5f, 1f, 0f) }

        val shard = ParticleMesh.shard(device)
        val rock = ParticleMesh.rock(device, seed = 3)
        val drop = ParticleMesh.sphere(device)
        val shards =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 512,
                        rate = 0f,
                        lifetime = 0.9f..1.3f,
                        shape = EmitterShape.Circle(0.3f),
                        direction = Vec3f(0f, 1f, 0f),
                        spread = 80.degrees,
                        speed = 0.5f..1.5f,
                        radialSpeed = 5f..9f,
                        gravity = Vec3f(0f, -6f, 0f),
                        drag = 3f,
                        size = 0.25f..0.6f,
                        sizes = listOf(1f, 1f, 0f),
                        colors = listOf(Color(0.75f, 0.9f, 1.1f, 1f)),
                        alignment = ParticleAlignment.VELOCITY,
                        stretch = 0.08f,
                        blend = ParticleBlend.OPAQUE,
                        mesh = shard,
                        shininess = 64f,
                    ),
                )
                .apply { position(2f, 0.2f, 0f) }
        val shockwave =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 8,
                        rate = 0f,
                        lifetime = 0.5f..0.5f,
                        speed = 0f..0f,
                        size = 6f..6f,
                        sizes = listOf(0.1f, 1f),
                        colors = listOf(Color(1f, 1f, 1f, 1f), Color(1f, 1f, 1f, 0f)),
                        rotation = 0f..0f,
                        blend = ParticleBlend.DISTORT,
                        texture = distortion.ringTexture,
                    ),
                )
                .apply { position(2f, 0.5f, 0f) }
        val rocks =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        maxParticles = 256,
                        rate = 0f,
                        lifetime = 1.2f..1.8f,
                        shape = EmitterShape.Circle(0.5f),
                        spread = 35.degrees,
                        speed = 4f..7f,
                        gravity = Vec3f(0f, -12f, 0f),
                        size = 0.12f..0.3f,
                        colors = listOf(Color(0.35f, 0.3f, 0.27f, 1f)),
                        angularVelocity = -400f..400f,
                        blend = ParticleBlend.OPAQUE,
                        mesh = rock,
                    ),
                )
                .apply { position(2f, 0.1f, 0f) }
        val molten =
            ParticleEmitter(
                    particles,
                    ParticleEffect(
                        rate = 40f,
                        lifetime = 0.8f..1.2f,
                        spread = 25.degrees,
                        speed = 4f..6f,
                        gravity = Vec3f(0f, -10f, 0f),
                        size = 0.08f..0.16f,
                        colors = listOf(Color(4f, 1.5f, 0.3f, 1f), Color(1.5f, 0.3f, 0.05f, 1f)),
                        alignment = ParticleAlignment.VELOCITY,
                        stretch = 0.12f,
                        blend = ParticleBlend.OPAQUE,
                        mesh = drop,
                        emissive = 0.9f,
                    ),
                )
                .apply { position(0f, 0f, -2.5f) }
        val scene = listOf(flames, shards, rocks, molten)
        val warps = listOf(haze, shockwave)
        val emitters = scene + warps

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        var time = 0f
        var nextNova = 0.5f
        onUpdate { dt ->
            time += dt.seconds
            camera.position.set(sin(time * 0.15f) * 6f, 3f, cos(time * 0.15f) * 6f)
            camera.up.set(Vec3f.Y_AXIS)
            camera.lookAt(0f, 1f, 0f)
            camera.update()
            if (input.isKeyJustPressed(Key.SPACE) || time >= nextNova) {
                shards.burst(160)
                rocks.burst(24)
                shockwave.burst(1)
                nextNova = time + 2f
            }
            floor.update(dt)
            emitters.forEach { it.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("VFX Particles Command Encoder") { frame, commandEncoder ->
                particles.simulate(commandEncoder, emitters, camera)
                val scenePass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(hdrTarget.view, Color(0.02f, 0.02f, 0.03f, 1f)),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "VFX Scene Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                modelBatch.flush(scenePass, camera, dt)
                particles.render(scenePass, camera, scene)
                scenePass.end()
                scenePass.release()

                val warpPass = distortion.beginPass(commandEncoder, hdrTarget.width, hdrTarget.height)
                particles.render(warpPass, camera, warps, depthTarget)
                warpPass.end()
                warpPass.release()

                bloom.render(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)
                postProcessor.process(commandEncoder, hdrTarget.view, hdrTarget.width, hdrTarget.height)
                val presentPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame), label = "VFX Present Pass")
                    )
                postProcessor.render(presentPass, preferredFormat)
                presentPass.end()
                presentPass.release()
            }
        }

        onRelease {
            emitters.forEach { it.release() }
            shard.release()
            rock.release()
            drop.release()
            flameSheet.release()
            particles.release()
            postProcessor.release()
            distortion.release()
            bloom.release()
            tonemapper.release()
            modelBatch.release()
            environment.release()
            floorMesh.release()
            floorMaterial.release()
        }
    }

    private fun createFlameSheet(device: Device): EmptyTexture {
        val cell = 64
        val size = cell * 4
        val pixels = ByteArray(size * size * 4)
        for (frame in 0 until 16) {
            val phase = frame / 16f * 6.2831853f
            val ox = (frame % 4) * cell
            val oy = (frame / 4) * cell
            for (y in 0 until cell) {
                for (x in 0 until cell) {
                    val u = (x + 0.5f) / cell * 2f - 1f
                    val v = 1f - (y + 0.5f) / cell * 2f
                    val rise = (v + 1f) * 0.5f
                    val sway = sin(rise * 7f + phase) * 0.18f * rise + sin(rise * 13f - phase * 2f) * 0.06f * rise
                    val width = 0.55f * (1f - rise * 0.85f) + 0.05f
                    val across = (u - sway) / width
                    val body = (1f - across * across).coerceAtLeast(0f) * (1f - rise).coerceAtLeast(0f)
                    val core = exp(-(across * across) * 3f) * exp(-rise * 2.5f)
                    val alpha = (body * 1.6f).coerceIn(0f, 1f) * sqrt((1f - rise).coerceAtLeast(0f)) * (rise * 5f).coerceAtMost(1f)
                    val i = ((oy + y) * size + ox + x) * 4
                    pixels[i] = byte(0.8f + core * 0.2f)
                    pixels[i + 1] = byte(0.45f + core * 0.5f)
                    pixels[i + 2] = byte(0.15f + core * 0.6f)
                    pixels[i + 3] = byte(alpha)
                }
            }
        }
        val texture =
            EmptyTexture(
                device,
                TextureFormat.RGBA8_UNORM,
                size,
                size,
                SamplerDescriptor(magFilter = FilterMode.LINEAR, minFilter = FilterMode.LINEAR),
                usage = TextureUsage.TEXTURE or TextureUsage.COPY_DST,
            )
        device.queue.writeTexture(
            ByteBuffer(pixels),
            TextureCopyView(texture.gpuTexture),
            TextureDataLayout(size * 4, size),
            Extent3D(size, size, 1),
        )
        return texture
    }

    private fun byte(value: Float): Byte = (value.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toByte()
}
