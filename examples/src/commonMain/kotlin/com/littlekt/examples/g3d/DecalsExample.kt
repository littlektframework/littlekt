package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.Decal
import com.littlekt.graphics.g3d.DecalRenderer
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
import com.littlekt.graphics.slice
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An example of [Decal]s and soft particles, both drawn from the depth of the opaque pass. A
 * targeting ring follows a moving unit over ramps & steps, scorch marks and a puddle sit on the
 * ground, and smoke fades where it meets geometry. Press S to toggle soft particles.
 *
 * @author Colton Daily
 * @date 11/15/2026
 */
class DecalsExample(context: Context) : ContextListener(context) {

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
                        intensity = 2f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.15f, 0.15f, 0.18f)))
            }
        val material =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.8f,
                baseColorTexture = Textures.textureWhite,
            )
        val floorMesh = fullIndexedMesh().generate {
            grid {
                sizeX = 30f
                sizeY = 30f
            }
        }
        val boxMesh = fullIndexedMesh().generate {
            cube { origin.set(-0.5f, -0.5f, -0.5f) }
        }
        val floor =
            MeshNode(listOf(MeshPrimitive(floorMesh, material))).apply {
                color(Color(0.45f, 0.47f, 0.5f, 1f))
            }
        val box = MeshNode(listOf(MeshPrimitive(boxMesh, material)))
        val tilt = MutableQuaternion()
        val props =
            listOf(
                box.copy().apply {
                    position(3f, 0.25f, 0f)
                    scaling(3f, 0.5f, 3f)
                },
                box.copy().apply {
                    position(-2f, 0.5f, 2f)
                    scaling(2f, 1f, 2f)
                    rotation(tilt.identity().rotate(20.degrees, Vec3f.Z_AXIS))
                },
                box.copy().apply {
                    position(0f, 1f, -4f)
                    scaling(1.5f, 2f, 1.5f)
                },
            )

        val ring = ringTexture(device)
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val decalRenderer =
            DecalRenderer(device).apply {
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val particles =
            ParticleRenderer(device).apply {
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }

        val target =
            Decal(ring.slice(), Color(1f, 0.4f, 0.2f, 0.9f)).apply { scaling(2.5f, 3f, 2.5f) }
        val dot = particles.defaultTexture.slice()
        val scorches =
            List(5) { i ->
                Decal(dot, Color(0.05f, 0.04f, 0.03f, 0.85f)).apply {
                    val angle = i * 1.3f
                    position(cos(angle) * 6f, 0f, sin(angle) * 6f + 1f)
                    val size = 2.5f + i * 0.5f
                    scaling(size, 1f, size)
                    rotate(y = (i * 47).degrees)
                }
            }
        val puddle =
            Decal(dot, Color(0.1f, 0.25f, 0.6f, 0.7f)).apply {
                position(-5f, 0f, -3f)
                scaling(4f, 1f, 2.5f)
            }
        val decals = scorches + puddle + target

        val smokeEffect =
            ParticleEffect(
                rate = 40f,
                lifetime = 2.5f..3.5f,
                shape = EmitterShape.Circle(1.3f),
                spread = 30.degrees,
                speed = 0.1f..0.3f,
                size = 1f..1.6f,
                sizes = listOf(0.8f, 1.4f),
                colors =
                    listOf(
                        Color(0.35f, 0.4f, 0.5f, 0f),
                        Color(0.35f, 0.4f, 0.5f, 0.45f),
                        Color(0.35f, 0.4f, 0.5f, 0f),
                    ),
                angularVelocity = -20f..20f,
                softness = 0.6f,
            )
        val smoke = ParticleEmitter(particles, smokeEffect).apply { position(3f, 0.5f, 0f) }
        val emitters = listOf(smoke)

        val clearColor = graphics.surfaceColor(Color(0.1f, 0.11f, 0.13f, 1f))
        var soft = true

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        var time = 0f
        onUpdate { dt ->
            time += dt.seconds
            camera.position.set(sin(time * 0.1f) * 13f, 9f, cos(time * 0.1f) * 13f)
            camera.up.set(Vec3f.Y_AXIS)
            camera.lookAt(0f, 0f, 0f)
            camera.update()
            if (input.isKeyJustPressed(Key.S)) {
                soft = !soft
                smoke.effect = smokeEffect.copy(softness = if (soft) 0.6f else 0f)
                logger.info { "Soft particles: $soft" }
            }
            target.position(cos(time * 0.6f) * 4f, 0.5f, sin(time * 0.6f) * 3f)
            target.rotation(tilt.identity().rotate((time * 1.5f).radians, Vec3f.Y_AXIS))
            floor.update(dt)
            props.forEach { it.update(dt) }
            decals.forEach { it.update(dt) }
            smoke.update(dt)
        }

        onUpdate { dt ->
            renderFrame("Decals Command Encoder") { frame, commandEncoder ->
                particles.simulate(commandEncoder, emitters)
                val opaque =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Opaque Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                props.forEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(opaque, camera, dt)
                opaque.end()
                opaque.release()

                val overlay =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame),
                            depthStencilAttachment = depthTarget.readOnlyDepthAttachment(),
                            label = "Decal & Particle Pass",
                        )
                    )
                decalRenderer.render(overlay, camera, depthTarget, decals)
                particles.render(overlay, camera, emitters, depthTarget)
                overlay.end()
                overlay.release()
            }
        }

        onRelease {
            smoke.release()
            particles.release()
            decalRenderer.release()
            modelBatch.release()
            environment.release()
            floorMesh.release()
            boxMesh.release()
            material.release()
            ring.release()
        }
    }

    private fun ringTexture(device: Device): EmptyTexture {
        val size = 128
        val pixels = ByteArray(size * size * 4)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x + 0.5f) / size * 2f - 1f
                val dy = (y + 0.5f) / size * 2f - 1f
                val distance = sqrt(dx * dx + dy * dy)
                val band = 1f - ((distance - 0.85f) / 0.08f).let { it * it }
                val alpha = band.coerceIn(0f, 1f)
                val i = (y * size + x) * 4
                pixels[i] = -1
                pixels[i + 1] = -1
                pixels[i + 2] = -1
                pixels[i + 3] = (alpha * 255f).toInt().toByte()
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
            pixels,
            TextureCopyView(texture.gpuTexture),
            TextureDataLayout(size * 4, size),
            Extent3D(size, size, 1),
            pixels.size.toLong(),
        )
        return texture
    }
}
