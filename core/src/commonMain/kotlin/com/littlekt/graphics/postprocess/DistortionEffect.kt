package com.littlekt.graphics.postprocess

import com.littlekt.file.ByteBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Bends the image where distortion was drawn, such as for heat haze & shockwaves. Each frame, draw
 * [com.littlekt.graphics.particle.ParticleBlend.DISTORT] particles into the pass from [beginPass],
 * then let the [PostProcessor] run this effect, before a [com.littlekt.graphics.Tonemapper].
 *
 * ```
 * val distortion = DistortionEffect(device)
 * postProcessor.effects.add(0, distortion)
 * val shockwave = ParticleEffect(texture = distortion.ringTexture, blend = ParticleBlend.DISTORT, sizes = listOf(0.2f, 1f))
 *
 * // each frame, after the scene pass
 * val pass = distortion.beginPass(commandEncoder, width, height)
 * particles.render(pass, camera, shockwaveEmitter, depth)
 * pass.end()
 * ```
 *
 * @author Colton Daily
 * @date 11/23/2026
 */
class DistortionEffect(device: Device) :
    ShaderPostEffect(
        device,
        """
        fn effect(uv: vec2f) -> vec4f {
            let push = textureSampleLevel(post_texture_0, source_sampler, uv, 0.0).xy;
            let offset = push * post.params[0].x * vec2f(post.resolution.y / post.resolution.x, 1.0);
            let spread = offset * post.params[0].y;
            let center = textureSampleLevel(source, source_sampler, uv + offset, 0.0);
            let red = textureSampleLevel(source, source_sampler, uv + offset + spread, 0.0).r;
            let blue = textureSampleLevel(source, source_sampler, uv + offset - spread, 0.0).b;
            return vec4f(red, center.g, blue, center.a);
        }
        """,
        paramCount = 2,
        label = "distortion effect",
        textureCount = 1,
    ) {

    /** How far a full strength push moves the image, as a fraction of the screen height. */
    var strength: Float
        get() = getParam(0)
        set(value) = setParam(0, value)

    /** How much red & blue split apart where the image bends, as a fraction of the push. */
    var chromatic: Float
        get() = getParam(1)
        set(value) = setParam(1, value)

    /** The format of the target distortion is drawn to. Must match `ParticleRenderer.distortionFormat`. */
    val format: TextureFormat = TextureFormat.RGBA16_FLOAT

    /** The target distortion is drawn to, sized by [beginPass]. */
    var target: EmptyTexture = createTarget(1, 1)
        private set

    /** A distortion map of a ring pushing outward, such as for shockwaves. */
    val ringTexture: Texture = createMap(MAP_SIZE) { x, y -> ring(x, y) }

    /** A distortion map of soft rippling noise fading out to its edges, such as for heat haze. */
    val hazeTexture: Texture = createMap(MAP_SIZE) { x, y -> haze(x, y) }

    private var passDescriptor: RenderPassDescriptor = createPassDescriptor()

    init {
        strength = 0.04f
        chromatic = 0.15f
        setTexture(0, target.view)
    }

    /**
     * Starts the pass distortion is drawn into, cleared to no distortion, resizing [target] to
     * [width] by [height] when needed. End it before the [PostProcessor] runs.
     */
    fun beginPass(commandEncoder: CommandEncoder, width: Int, height: Int): RenderPassEncoder {
        if (width != target.width || height != target.height) {
            target.release()
            target = createTarget(width, height)
            passDescriptor = createPassDescriptor()
            setTexture(0, target.view)
        }
        return commandEncoder.beginRenderPass(passDescriptor)
    }

    private fun createTarget(width: Int, height: Int): EmptyTexture =
        EmptyTexture(
            device,
            format,
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            usage = TextureUsage.TEXTURE or TextureUsage.RENDER_ATTACHMENT,
        )

    private fun createPassDescriptor(): RenderPassDescriptor =
        RenderPassDescriptor(
            listOf(
                RenderPassColorAttachmentDescriptor(
                    view = target.view,
                    loadOp = LoadOp.CLEAR,
                    storeOp = StoreOp.STORE,
                    clearColor = Color.CLEAR,
                )
            ),
            label = "distortion pass",
        )

    private inline fun createMap(size: Int, push: (Float, Float) -> FloatArray): Texture {
        val pixels = ByteArray(size * size * 4)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val value = push((x + 0.5f) / size * 2f - 1f, (y + 0.5f) / size * 2f - 1f)
                val i = (y * size + x) * 4
                pixels[i] = toByte(value[0] * 0.5f + 0.5f)
                pixels[i + 1] = toByte(value[1] * 0.5f + 0.5f)
                pixels[i + 2] = 0
                pixels[i + 3] = toByte(value[2])
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

    private fun toByte(value: Float): Byte = (value.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toByte()

    private fun ring(x: Float, y: Float): FloatArray {
        val r = sqrt(x * x + y * y)
        if (r < 0.0001f || r > 1f) return floatArrayOf(0f, 0f, 0f)
        val band = (r - RING_RADIUS) / RING_WIDTH
        return floatArrayOf(x / r, y / r, exp(-band * band) * (1f - r * r).coerceAtLeast(0f) * 2f)
    }

    private fun haze(x: Float, y: Float): FloatArray {
        val r2 = x * x + y * y
        if (r2 > 1f) return floatArrayOf(0f, 0f, 0f)
        val dx = sin(x * 9.1f + y * 4.3f) + 0.5f * sin(x * 17.3f - y * 11.7f)
        val dy = sin(y * 8.7f - x * 5.1f) + 0.5f * sin(y * 15.9f + x * 12.3f)
        val length = sqrt(dx * dx + dy * dy).let { if (it > 0f) it else 1f }
        val falloff = (1f - r2) * (1f - r2)
        return floatArrayOf(dx / length, dy / length, falloff)
    }

    override fun release() {
        super.release()
        target.release()
        ringTexture.release()
        hazeTexture.release()
    }

    private companion object {
        const val MAP_SIZE = 128
        const val RING_RADIUS = 0.72f
        const val RING_WIDTH = 0.12f
    }
}
