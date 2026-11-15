package com.littlekt.graphics.particle

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.util.align
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Simulates [ParticleEmitter]s with a compute pass and draws them as camera facing quads. Set
 * [colorFormat], [depthFormat] & [sampleCount] to match the render pass, like a
 * [com.littlekt.graphics.g3d.ModelBatch].
 *
 * Works in 2D with an orthographic camera and particles on the `z = 0` plane, such as through a
 * scene graph `GpuParticles2D` node. Set [depthFormat] to `null` when the pass has no depth target.
 *
 * Particles are tested against the depth buffer without writing to it, so draw them after opaque
 * geometry.
 *
 * ```
 * renderFrame { frame, commandEncoder ->
 *     particles.simulate(commandEncoder, emitter)
 *     val renderPass = commandEncoder.beginRenderPass(...)
 *     modelBatch.flush(renderPass, camera, dt)
 *     particles.render(renderPass, camera, emitter)
 *     renderPass.end()
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/14/2026
 */
class ParticleRenderer(val device: Device) : Releasable {

    /** The format of the color target particles are drawn to. */
    var colorFormat: TextureFormat = TextureFormat.BGRA8_UNORM_SRGB

    /** The format of the depth target, or `null` if the render pass has none. */
    var depthFormat: TextureFormat? = TextureFormat.DEPTH24_PLUS_STENCIL8

    /** The number of samples of the render pass targets, `1` or `4`. */
    var sampleCount: Int = 1
        set(value) {
            require(value == 1 || value == 4) { "ParticleRenderer.sampleCount must be 1 or 4!" }
            field = value
        }

    /** The most cameras particles can be drawn with in a single frame. */
    var maxViewsPerFrame: Int = 8
        set(value) {
            require(value > 0) { "maxViewsPerFrame must be greater than 0!" }
            if (value == field) return
            field = value
            recreateCameraBuffer()
        }

    internal val computeLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.COMPUTE, BufferBindingLayout()),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                ),
                "ParticleRenderer compute layout",
            )
        )

    internal val emitterLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT,
                        BufferBindingLayout(),
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                ),
                "ParticleRenderer emitter layout",
            )
        )

    private val cameraStride =
        (CAMERA_FLOATS * Float.SIZE_BYTES).align(device.limits.minUniformBufferOffsetAlignment)

    private val cameraLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT,
                        BufferBindingLayout(
                            hasDynamicOffset = true,
                            minBindingSize = CAMERA_FLOATS * Float.SIZE_BYTES.toLong(),
                        ),
                    )
                ),
                "ParticleRenderer camera layout",
            )
        )

    private val textureLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                "ParticleRenderer texture layout",
            )
        )

    private val depthLayout = createDepthLayout(multisampled = false)
    private val msaaDepthLayout = createDepthLayout(multisampled = true)

    private val computeShader = device.createShaderModule(COMPUTE_WGSL)
    private val renderShader = device.createShaderModule(RENDER_WGSL)
    private val msaaSoftShader =
        device.createShaderModule(
            RENDER_WGSL.replace("texture_depth_2d", "texture_depth_multisampled_2d")
        )
    private val computePipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(computeLayout), "ParticleRenderer compute pipeline")
        )
    private val renderPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout),
                "ParticleRenderer render pipeline",
            )
        )
    private val softPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout, depthLayout),
                "ParticleRenderer soft render pipeline",
            )
        )
    private val softMsaaPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, emitterLayout, textureLayout, msaaDepthLayout),
                "ParticleRenderer soft msaa render pipeline",
            )
        )
    private var depthBindGroup: BindGroup? = null
    private var depthBindGroupId = -1
    private val computePipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                computePipelineLayout,
                ProgrammableStage(computeShader, "simulate"),
                "ParticleRenderer simulate",
            )
        )
    private val pipelines = mutableListOf<PipelineEntry>()

    /** The soft round dot used when [ParticleEffect.texture] is `null`. */
    val defaultTexture: Texture = createDefaultTexture()

    private var cameraBuffer: GPUBuffer = createCameraBuffer()
    private var cameraBindGroup: BindGroup = createCameraBindGroup()
    private val cameraData = FloatBuffer(CAMERA_FLOATS)
    private val cameraViews = mutableListOf<FloatArray>()
    private val cameraRefs = mutableListOf<Camera?>()
    private var cameraSizes = LongArray(4)
    private var viewFrame = -1L
    private var viewCount = 0

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    /**
     * Spawns & moves the particles of [emitter]. Call once per frame after the emitter is updated
     * and before the render pass that draws it.
     */
    fun simulate(commandEncoder: CommandEncoder, emitter: ParticleEmitter) {
        val slots = emitter.prepare()
        if (slots == 0) return
        val pass = commandEncoder.beginComputePass("ParticleRenderer simulate")
        pass.setPipeline(computePipeline)
        pass.setBindGroup(0, emitter.computeBindGroup)
        pass.dispatchWorkgroups(ceil(slots / WORKGROUP_SIZE.toFloat()).toInt())
        pass.end()
        pass.release()
    }

    /** Simulates each of [emitters] in one compute pass. @see simulate */
    fun simulate(commandEncoder: CommandEncoder, emitters: List<ParticleEmitter>) {
        var pass: ComputePassEncoder? = null
        for (i in emitters.indices) {
            val emitter = emitters[i]
            val slots = emitter.prepare()
            if (slots == 0) continue
            val encoder =
                pass
                    ?: commandEncoder.beginComputePass("ParticleRenderer simulate").also {
                        it.setPipeline(computePipeline)
                        pass = it
                    }
            encoder.setBindGroup(0, emitter.computeBindGroup)
            encoder.dispatchWorkgroups(ceil(slots / WORKGROUP_SIZE.toFloat()).toInt())
        }
        pass?.let {
            it.end()
            it.release()
        }
    }

    /**
     * Draws the particles of [emitter] with [camera] within [renderPassEncoder].
     *
     * @param depth the depth texture of the opaque geometry, attached to the pass with
     *   [EmptyTexture.readOnlyDepthAttachment], so particles with a [ParticleEffect.softness] fade
     *   where they meet it. `null` draws every particle hard.
     */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        camera: Camera,
        emitter: ParticleEmitter,
        depth: EmptyTexture? = null,
    ) {
        val count = emitter.activeCount
        if (count == 0) return
        val soft = depth != null && emitter.effect.softness > 0f
        val msaaDepth = soft && depth != null && depth.textureDescriptor.sampleCount > 1
        val offset = cameraOffset(camera, depth?.width ?: 0, depth?.height ?: 0)
        renderPassEncoder.setPipeline(pipeline(emitter.effect.blend, soft, msaaDepth))
        renderPassEncoder.setBindGroup(0, cameraBindGroup, offset)
        renderPassEncoder.setBindGroup(1, emitter.renderBindGroup)
        renderPassEncoder.setBindGroup(2, emitter.textureBindGroup)
        if (soft && depth != null) renderPassEncoder.setBindGroup(3, depthBindGroup(depth, msaaDepth))
        renderPassEncoder.draw(6, count)
    }

    /** Draws each of [emitters] with [camera] within [renderPassEncoder]. @see render */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        camera: Camera,
        emitters: List<ParticleEmitter>,
        depth: EmptyTexture? = null,
    ) {
        for (i in emitters.indices) render(renderPassEncoder, camera, emitters[i], depth)
    }

    private fun depthBindGroup(depth: EmptyTexture, multisampled: Boolean): BindGroup {
        val current = depthBindGroup
        if (current != null && depthBindGroupId == depth.id) return current
        current?.release()
        depthBindGroupId = depth.id
        return device
            .createBindGroup(
                BindGroupDescriptor(
                    if (multisampled) msaaDepthLayout else depthLayout,
                    listOf(BindGroupEntry(0, depth.depthView)),
                    label = "ParticleRenderer depth bind group",
                )
            )
            .also { depthBindGroup = it }
    }

    internal fun createTextureBindGroup(texture: Texture?): BindGroup {
        val source = texture ?: defaultTexture
        return device.createBindGroup(
            BindGroupDescriptor(
                textureLayout,
                listOf(BindGroupEntry(0, source.view), BindGroupEntry(1, source.sampler)),
                label = "ParticleRenderer texture bind group",
            )
        )
    }

    private fun cameraOffset(camera: Camera, width: Int, height: Int): Int {
        val frame = EngineStats.frame.value
        if (frame != viewFrame) {
            viewFrame = frame
            viewCount = 0
        }
        val viewProjection = camera.viewProjection.data
        val size = (width.toLong() shl 32) or (height.toLong() and 0xFFFFFFFFL)
        for (i in 0 until viewCount) {
            if (
                cameraRefs[i] === camera &&
                    cameraSizes[i] == size &&
                    cameraViews[i].contentEquals(viewProjection)
            ) {
                return i * cameraStride
            }
        }
        if (viewCount >= maxViewsPerFrame) {
            logger.warn {
                "ParticleRenderer has exceeded maxViewsPerFrame ($maxViewsPerFrame). Reusing the last view which will overwrite its data!"
            }
            viewCount = maxViewsPerFrame - 1
        }
        val index = viewCount++
        while (cameraViews.size <= index) {
            cameraViews += FloatArray(16)
            cameraRefs += null
            if (cameraSizes.size <= index) cameraSizes = cameraSizes.copyOf(index * 2 + 1)
        }
        viewProjection.copyInto(cameraViews[index])
        cameraRefs[index] = camera
        cameraSizes[index] = size
        writeCamera(camera, index, width, height)
        return index * cameraStride
    }

    private fun writeCamera(camera: Camera, index: Int, width: Int, height: Int) {
        val viewProjection = camera.viewProjection.data
        for (i in 0 until 16) cameraData[i] = viewProjection[i]
        val view = camera.view.data
        setNormalized(16, view[0], view[4], view[8])
        setNormalized(20, view[1], view[5], view[9])
        setNormalized(24, view[2], view[6], view[10])
        val inverseProjection = camera.invProj.data
        for (i in 0 until 16) cameraData[28 + i] = inverseProjection[i]
        cameraData[44] = width.toFloat()
        cameraData[45] = height.toFloat()
        cameraData[46] = 0f
        cameraData[47] = 0f
        device.queue.writeBuffer(cameraBuffer, cameraData, offset = index.toLong() * cameraStride)
    }

    private fun setNormalized(offset: Int, x: Float, y: Float, z: Float) {
        val length = sqrt(x * x + y * y + z * z).let { if (it > 0f) it else 1f }
        cameraData[offset] = x / length
        cameraData[offset + 1] = y / length
        cameraData[offset + 2] = z / length
        cameraData[offset + 3] = 0f
    }

    private fun pipeline(blend: ParticleBlend, soft: Boolean, msaaDepth: Boolean): RenderPipeline {
        val depth = depthFormat
        for (i in pipelines.indices) {
            val entry = pipelines[i]
            if (
                entry.blend == blend &&
                entry.soft == soft &&
                entry.msaaDepth == msaaDepth &&
                entry.colorFormat == colorFormat &&
                entry.depthFormat == depth &&
                entry.sampleCount == sampleCount
            ) {
                return entry.pipeline
            }
        }
        val pipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout =
                        when {
                            msaaDepth -> softMsaaPipelineLayout
                            soft -> softPipelineLayout
                            else -> renderPipelineLayout
                        },
                    vertex = VertexState(if (msaaDepth) msaaSoftShader else renderShader, "vs_main"),
                    fragment =
                        FragmentState(
                            if (msaaDepth) msaaSoftShader else renderShader,
                            if (soft) "fs_soft" else "fs_main",
                            listOf(
                                ColorTargetState(
                                    colorFormat,
                                    when (blend) {
                                        ParticleBlend.ALPHA -> BlendState.NonPreMultiplied
                                        ParticleBlend.ADDITIVE -> BlendState.Add
                                    },
                                )
                            ),
                        ),
                    depthStencil =
                        depth?.let {
                            DepthStencilState(
                                format = it,
                                depthWriteEnabled = false,
                                depthCompare = CompareFunction.LESS_EQUAL,
                            )
                        },
                    multisample = MultisampleState(count = sampleCount),
                    label = "ParticleRenderer $blend pipeline",
                )
            )
        pipelines += PipelineEntry(blend, soft, msaaDepth, colorFormat, depth, sampleCount, pipeline)
        return pipeline
    }

    private fun createDefaultTexture(): Texture {
        val size = DEFAULT_TEXTURE_SIZE
        val pixels = ByteArray(size * size * 4)
        val half = size / 2f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x + 0.5f - half) / half
                val dy = (y + 0.5f - half) / half
                val falloff = (1f - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
                val i = (y * size + x) * 4
                pixels[i] = WHITE
                pixels[i + 1] = WHITE
                pixels[i + 2] = WHITE
                pixels[i + 3] = (falloff * falloff * 255f).toInt().toByte()
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

    private fun createCameraBuffer(): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleRenderer camera",
                maxViewsPerFrame.toLong() * cameraStride,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createCameraBindGroup(): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                cameraLayout,
                listOf(
                    BindGroupEntry(
                        0,
                        BufferBinding(cameraBuffer, 0, CAMERA_FLOATS * Float.SIZE_BYTES.toLong()),
                    )
                ),
                label = "ParticleRenderer camera bind group",
            )
        )

    private fun recreateCameraBuffer() {
        cameraBindGroup.release()
        cameraBuffer.release()
        cameraBuffer = createCameraBuffer()
        cameraBindGroup = createCameraBindGroup()
        viewFrame = -1L
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        pipelines.forEach { it.pipeline.release() }
        pipelines.clear()
        cameraBindGroup.release()
        cameraBuffer.release()
        defaultTexture.release()
        computePipeline.release()
        computePipelineLayout.release()
        renderPipelineLayout.release()
        softPipelineLayout.release()
        softMsaaPipelineLayout.release()
        msaaSoftShader.release()
        msaaDepthLayout.release()
        depthBindGroup?.release()
        depthLayout.release()
        computeShader.release()
        renderShader.release()
        computeLayout.release()
        emitterLayout.release()
        cameraLayout.release()
        textureLayout.release()
    }

    private fun createDepthLayout(multisampled: Boolean): BindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.FRAGMENT,
                        TextureBindingLayout(
                            sampleType = TextureSampleType.DEPTH,
                            multisampled = multisampled,
                        ),
                    )
                ),
                "ParticleRenderer depth layout",
            )
        )

    private class PipelineEntry(
        val blend: ParticleBlend,
        val soft: Boolean,
        val msaaDepth: Boolean,
        val colorFormat: TextureFormat,
        val depthFormat: TextureFormat?,
        val sampleCount: Int,
        val pipeline: RenderPipeline,
    )

    private companion object {
        const val CAMERA_FLOATS = 48
        const val WORKGROUP_SIZE = 64
        const val DEFAULT_TEXTURE_SIZE = 64
        const val WHITE = 0xFF.toByte()
        val logger = Logger<ParticleRenderer>()

        const val PARAMS_WGSL =
            """
struct Params {
    transform: mat4x4f,
    previous: vec4f,
    spawn: vec4f,
    gravity: vec4f,
    direction: vec4f,
    shape: vec4f,
    life_speed: vec4f,
    size_radial: vec4f,
    rotation: vec4f,
    flags: vec4f,
    sizes: vec4f,
    colors: array<vec4f, 4>,
    extra: vec4f,
    plane: vec4f,
};

struct Particle {
    position: vec4f,
    velocity: vec4f,
    misc: vec4f,
};
"""

        const val COMPUTE_WGSL =
            PARAMS_WGSL +
                    """
@group(0) @binding(0) var<uniform> params: Params;
@group(0) @binding(1) var<storage, read_write> particles: array<Particle>;

fn hash(value: u32) -> u32 {
    var state = value * 747796405u + 2891336453u;
    let word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
    return (word >> 22u) ^ word;
}

fn random(seed: ptr<function, u32>) -> f32 {
    *seed = hash(*seed);
    return f32(*seed) / 4294967295.0;
}

fn random_range(seed: ptr<function, u32>, range: vec2f) -> f32 {
    return mix(range.x, range.y, random(seed));
}

fn random_direction(seed: ptr<function, u32>) -> vec3f {
    let z = random(seed) * 2.0 - 1.0;
    let angle = random(seed) * 6.2831853;
    let r = sqrt(max(0.0, 1.0 - z * z));
    return vec3f(r * cos(angle), r * sin(angle), z);
}

fn cone_direction(seed: ptr<function, u32>, axis: vec3f, cos_spread: f32) -> vec3f {
    let z = mix(1.0, cos_spread, random(seed));
    let angle = random(seed) * 6.2831853;
    let r = sqrt(max(0.0, 1.0 - z * z));
    let up = select(vec3f(0.0, 1.0, 0.0), vec3f(1.0, 0.0, 0.0), abs(axis.y) > 0.99);
    let tangent = normalize(cross(up, axis));
    let bitangent = cross(axis, tangent);
    return tangent * (r * cos(angle)) + bitangent * (r * sin(angle)) + axis * z;
}

fn is_flat() -> bool {
    return params.plane.x > 0.5;
}

fn flat_disc(seed: ptr<function, u32>, radius: f32, edge: bool) -> vec3f {
    let angle = random(seed) * 6.2831853;
    let r = select(radius * sqrt(random(seed)), radius, edge);
    return vec3f(cos(angle) * r, sin(angle) * r, 0.0);
}

fn flat_direction(seed: ptr<function, u32>, axis: vec3f, cos_spread: f32) -> vec3f {
    let spread = acos(clamp(cos_spread, -1.0, 1.0));
    let angle = atan2(axis.y, axis.x) + (random(seed) * 2.0 - 1.0) * spread;
    return vec3f(cos(angle), sin(angle), 0.0);
}

fn spawn_point(seed: ptr<function, u32>) -> vec3f {
    let shape = params.shape;
    let flag = params.extra.z;
    switch (u32(shape.x)) {
        case 1u: {
            if (is_flat()) {
                return flat_disc(seed, shape.y, flag > 0.5);
            }
            let radius = select(shape.y * pow(random(seed), 1.0 / 3.0), shape.y, flag > 0.5);
            return random_direction(seed) * radius;
        }
        case 2u: {
            let point = (vec3f(random(seed), random(seed), random(seed)) * 2.0 - 1.0) * shape.yzw;
            return select(point, vec3f(point.xy, 0.0), is_flat());
        }
        case 3u: {
            if (is_flat()) {
                return flat_disc(seed, shape.y, flag > 0.5);
            }
            let angle = random(seed) * 6.2831853;
            let radius = select(shape.y * sqrt(random(seed)), shape.y, flag > 0.5);
            return vec3f(cos(angle) * radius, 0.0, sin(angle) * radius);
        }
        default: {
            return vec3f(0.0);
        }
    }
}

@compute @workgroup_size(64)
fn simulate(@builtin(global_invocation_id) id: vec3u) {
    let capacity = u32(params.spawn.z);
    let index = id.x;
    if (index >= capacity) {
        return;
    }
    var p = particles[index];
    if (params.extra.x > 0.5) {
        p.position.w = 0.0;
        p.velocity.w = 0.0;
    }
    let dt = params.previous.w;
    let start = u32(params.spawn.x);
    let count = u32(params.spawn.y);
    let relative = (index + capacity - start) % capacity;
    if (relative < count) {
        var seed = hash(index ^ hash(u32(params.spawn.w) + relative * 1013u));
        let t = (f32(relative) + random(&seed)) / f32(count);
        let local_space = params.flags.x > 0.5;
        let local_point = spawn_point(&seed);
        let axis = normalize(params.direction.xyz);
        var direction = cone_direction(&seed, axis, params.direction.w);
        if (is_flat()) {
            direction = flat_direction(&seed, axis, params.direction.w);
        }
        var velocity = direction * random_range(&seed, params.life_speed.zw);
        let radial = random_range(&seed, params.size_radial.zw);
        if (radial != 0.0 && length(local_point) > 0.0001) {
            velocity += normalize(local_point) * radial;
        }
        var position = local_point;
        if (!local_space) {
            position = (params.transform * vec4f(local_point, 1.0)).xyz;
            velocity = (params.transform * vec4f(velocity, 0.0)).xyz;
            let current = params.transform[3].xyz;
            position += mix(params.previous.xyz, current, t) - current;
            if (dt > 0.0) {
                velocity += (current - params.previous.xyz) / dt * params.extra.w;
            }
        }
        if (is_flat()) {
            velocity.z = 0.0;
            position.z = select(params.transform[3].z, 0.0, local_space);
        }
        let age = (1.0 - t) * dt;
        p.position = vec4f(position + velocity * age, age);
        p.velocity = vec4f(velocity, random_range(&seed, params.life_speed.xy));
        p.misc = vec4f(
            random_range(&seed, params.size_radial.xy),
            random_range(&seed, params.rotation.xy),
            random_range(&seed, params.rotation.zw),
            random(&seed)
        );
    } else if (p.position.w < p.velocity.w) {
        var velocity = p.velocity.xyz + params.gravity.xyz * dt;
        velocity = velocity / (1.0 + params.gravity.w * dt);
        if (is_flat()) {
            velocity.z = 0.0;
        }
        p.position = vec4f(p.position.xyz + velocity * dt, p.position.w + dt);
        p.velocity = vec4f(velocity, p.velocity.w);
        p.misc.y += p.misc.z * dt;
    }
    particles[index] = p;
}
"""

        const val RENDER_WGSL =
            PARAMS_WGSL +
                    """
struct Camera {
    view_proj: mat4x4f,
    right: vec4f,
    up: vec4f,
    back: vec4f,
    inv_proj: mat4x4f,
    viewport: vec4f,
};

@group(0) @binding(0) var<uniform> camera: Camera;
@group(1) @binding(0) var<uniform> params: Params;
@group(1) @binding(1) var<storage, read> particles: array<Particle>;
@group(2) @binding(0) var particle_texture: texture_2d<f32>;
@group(2) @binding(1) var particle_sampler: sampler;
@group(3) @binding(0) var scene_depth: texture_depth_2d;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
    @location(1) uv: vec2f,
};

fn key_index(t: f32, count: f32) -> vec2f {
    let scaled = clamp(t, 0.0, 1.0) * max(count - 1.0, 0.0);
    let index = min(floor(scaled), max(count - 2.0, 0.0));
    return vec2f(index, scaled - index);
}

fn size_at(t: f32) -> f32 {
    let count = params.flags.w;
    if (count < 1.5) {
        return params.sizes.x;
    }
    let key = key_index(t, count);
    let a = params.sizes[u32(key.x)];
    let b = params.sizes[u32(key.x) + 1u];
    return mix(a, b, clamp(key.y, 0.0, 1.0));
}

fn color_at(t: f32) -> vec4f {
    let count = params.flags.z;
    if (count < 1.5) {
        return params.colors[0];
    }
    let key = key_index(t, count);
    return mix(params.colors[u32(key.x)], params.colors[u32(key.x) + 1u], clamp(key.y, 0.0, 1.0));
}

@vertex
fn vs_main(@builtin(vertex_index) vertex: u32, @builtin(instance_index) instance: u32) -> VertexOutput {
    var output: VertexOutput;
    let p = particles[instance];
    if (p.position.w >= p.velocity.w) {
        output.position = vec4f(0.0, 0.0, 2.0, 1.0);
        output.color = vec4f(0.0);
        output.uv = vec2f(0.0);
        return output;
    }
    var corners = array<vec2f, 6>(
        vec2f(-0.5, -0.5), vec2f(0.5, -0.5), vec2f(0.5, 0.5),
        vec2f(-0.5, -0.5), vec2f(0.5, 0.5), vec2f(-0.5, 0.5)
    );
    let corner = corners[vertex];
    let t = p.position.w / p.velocity.w;
    let size = p.misc.x * size_at(t);
    let c = cos(p.misc.y);
    let s = sin(p.misc.y);
    let rotated = vec2f(corner.x * c - corner.y * s, corner.x * s + corner.y * c);
    var center = p.position.xyz;
    var velocity = p.velocity.xyz;
    if (params.flags.x > 0.5) {
        center = (params.transform * vec4f(center, 1.0)).xyz;
        velocity = (params.transform * vec4f(velocity, 0.0)).xyz;
    }
    var offset: vec3f;
    switch (u32(params.flags.y)) {
        case 1u: {
            let speed = length(velocity);
            let axis = select(camera.up.xyz, velocity / speed, speed > 0.0001);
            var side = cross(axis, camera.back.xyz);
            side = select(camera.right.xyz, normalize(side), length(side) > 0.0001);
            offset = side * (corner.x * size) + axis * (corner.y * size * (1.0 + speed * params.extra.y));
        }
        case 2u: {
            offset = vec3f(rotated.x, 0.0, rotated.y) * size;
        }
        default: {
            offset = camera.right.xyz * (rotated.x * size) + camera.up.xyz * (rotated.y * size);
        }
    }
    output.position = camera.view_proj * vec4f(center + offset, 1.0);
    output.color = color_at(t);
    output.uv = vec2f(corner.x + 0.5, 0.5 - corner.y);
    return output;
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4f {
    let color = textureSample(particle_texture, particle_sampler, input.uv) * input.color;
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}

fn view_depth(position: vec2f, depth: f32) -> f32 {
    let ndc = vec4f(
        position.x / camera.viewport.x * 2.0 - 1.0,
        1.0 - position.y / camera.viewport.y * 2.0,
        depth,
        1.0
    );
    let view = camera.inv_proj * ndc;
    return view.z / view.w;
}

@fragment
fn fs_soft(input: VertexOutput) -> @location(0) vec4f {
    var color = textureSample(particle_texture, particle_sampler, input.uv) * input.color;
    let scene = textureLoad(scene_depth, vec2i(input.position.xy), 0);
    let gap = view_depth(input.position.xy, input.position.z) - view_depth(input.position.xy, scene);
    color.a *= clamp(gap / params.plane.y, 0.0, 1.0);
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}
"""
    }
}
