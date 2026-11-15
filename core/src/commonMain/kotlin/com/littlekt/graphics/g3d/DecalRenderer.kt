package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.util.align
import com.littlekt.util.datastructure.IntMap
import kotlin.math.cos
import kotlin.math.max

/**
 * Draws [Decal]s onto the geometry already in a depth texture. Render the opaque geometry first,
 * then begin a pass that attaches the same depth with [EmptyTexture.readOnlyDepthAttachment] and
 * draw the decals in it. Set [colorFormat] & [depthFormat] to match that pass.
 *
 * ```
 * val pass = commandEncoder.beginRenderPass(
 *     RenderPassDescriptor(
 *         colorAttachment(frame),
 *         depthStencilAttachment = depthTarget.readOnlyDepthAttachment(),
 *     )
 * )
 * decals.render(pass, camera, depthTarget, scorchMarks)
 * ```
 *
 * The depth texture needs [TextureUsage.TEXTURE]. A multisampled depth texture is read from its
 * first sample, and the pipelines match its sample count.
 *
 * @param device the device to create buffers & pipelines with
 * @param initialDecals the number of decals the buffers start with room for. They grow as needed.
 * @author Colton Daily
 * @date 11/15/2026
 */
class DecalRenderer(val device: Device, initialDecals: Int = 64) : Releasable {

    /** The format of the color target. */
    var colorFormat: TextureFormat = TextureFormat.BGRA8_UNORM_SRGB

    /** The format of the depth texture attached to the pass. */
    var depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8

    /** The blending of decals with the surfaces under them. */
    var blendState: BlendState = BlendState.NonPreMultiplied

    /** The most calls to [render] in a single frame. */
    var maxViewsPerFrame: Int = 8
        set(value) {
            require(value > 0) { "maxViewsPerFrame must be greater than 0!" }
            if (value == field) return
            field = value
            retired += cameraBuffer
            cameraBindGroupDirty = true
            cameraBuffer = createCameraBuffer()
        }

    private val cameraStride =
        (CAMERA_FLOATS * Float.SIZE_BYTES).align(device.limits.minUniformBufferOffsetAlignment)
    private val frameLayout =
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
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                ),
                "DecalRenderer frame layout",
            )
        )
    private val depthLayout = createDepthLayout(multisampled = false)
    private val msaaDepthLayout = createDepthLayout(multisampled = true)
    private val textureLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                "DecalRenderer texture layout",
            )
        )
    private val shader = device.createShaderModule(wgsl(multisampled = false))
    private val msaaShader = device.createShaderModule(wgsl(multisampled = true))
    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(frameLayout, depthLayout, textureLayout), "DecalRenderer")
        )
    private val msaaPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(frameLayout, msaaDepthLayout, textureLayout),
                "DecalRenderer msaa",
            )
        )
    private val pipelines = mutableListOf<PipelineEntry>()
    private val textureBindGroups = IntMap<BindGroup>()
    private var depthBindGroup: BindGroup? = null
    private var depthBindGroupId = -1
    private val retired = mutableListOf<GPUBuffer>()

    private var cameraBuffer = createCameraBuffer()
    private var instanceCapacity = max(initialDecals, 1)
    private var instanceBuffer = createInstanceBuffer(instanceCapacity)
    private var frameBindGroup: BindGroup? = null
    private var cameraBindGroupDirty = true
    private val cameraData = FloatBuffer(CAMERA_FLOATS)
    private var instanceData = FloatBuffer(instanceCapacity * INSTANCE_FLOATS)
    private var frame = -1L
    private var viewIndex = 0
    private var instanceOffset = 0
    private val single = ArrayList<Decal>(1)

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    /** Draws [decal] onto the geometry in [depth] within [renderPassEncoder]. */
    fun render(renderPassEncoder: RenderPassEncoder, camera: Camera, depth: EmptyTexture, decal: Decal) {
        single.clear()
        single += decal
        render(renderPassEncoder, camera, depth, single)
        single.clear()
    }

    /**
     * Draws each of [decals] onto the geometry in [depth] within [renderPassEncoder]. Decals with
     * the same texture next to each other are drawn together.
     */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        camera: Camera,
        depth: EmptyTexture,
        decals: List<Decal>,
    ) {
        if (decals.isEmpty()) return
        nextFrameCheck()
        if (instanceOffset + decals.size > instanceCapacity) grow(decals.size)
        if (instanceData.capacity < decals.size * INSTANCE_FLOATS) {
            instanceData = FloatBuffer(max(instanceData.capacity * 2, decals.size * INSTANCE_FLOATS))
        }
        for (i in decals.indices) writeInstance(i, decals[i])
        device.queue.writeBuffer(
            instanceBuffer,
            instanceData,
            offset = instanceOffset.toLong() * INSTANCE_FLOATS * Float.SIZE_BYTES,
            size = decals.size.toLong() * INSTANCE_FLOATS,
        )
        if (viewIndex >= maxViewsPerFrame) {
            logger.warn {
                "DecalRenderer has exceeded maxViewsPerFrame ($maxViewsPerFrame). Reusing the last view which will overwrite its data!"
            }
            viewIndex = maxViewsPerFrame - 1
        }
        val view = viewIndex++
        writeCamera(camera, depth, view)

        val samples = depth.textureDescriptor.sampleCount
        renderPassEncoder.setPipeline(pipeline(samples))
        renderPassEncoder.setBindGroup(0, frameBindGroup(), view * cameraStride)
        renderPassEncoder.setBindGroup(1, depthBindGroup(depth, samples > 1))
        var runStart = 0
        while (runStart < decals.size) {
            val texture = decals[runStart].slice.texture
            var runEnd = runStart + 1
            while (runEnd < decals.size && decals[runEnd].slice.texture === texture) runEnd++
            renderPassEncoder.setBindGroup(2, textureBindGroup(texture))
            renderPassEncoder.draw(36, runEnd - runStart, 0, instanceOffset + runStart)
            runStart = runEnd
        }
        instanceOffset += decals.size
    }

    private fun writeInstance(index: Int, decal: Decal) {
        val o = index * INSTANCE_FLOATS
        val model = decal.globalTransform.data
        val inverse = decal.globalInverseTransform.data
        for (i in 0 until 16) {
            instanceData[o + i] = model[i]
            instanceData[o + 16 + i] = inverse[i]
        }
        instanceData[o + 32] = model[4]
        instanceData[o + 33] = model[5]
        instanceData[o + 34] = model[6]
        instanceData[o + 35] = 0f
        val color = decal.color
        instanceData[o + 36] = color.r
        instanceData[o + 37] = color.g
        instanceData[o + 38] = color.b
        instanceData[o + 39] = color.a
        val slice = decal.slice
        instanceData[o + 40] = slice.u
        instanceData[o + 41] = slice.v
        instanceData[o + 42] = slice.u1
        instanceData[o + 43] = slice.v1
        instanceData[o + 44] = cos(decal.angleFade.radians)
        instanceData[o + 45] = decal.edgeFade
        instanceData[o + 46] = 0f
        instanceData[o + 47] = 0f
    }

    private fun writeCamera(camera: Camera, depth: EmptyTexture, view: Int) {
        val viewProjection = camera.viewProjection.data
        val inverse = camera.invViewProjection.data
        for (i in 0 until 16) {
            cameraData[i] = viewProjection[i]
            cameraData[16 + i] = inverse[i]
        }
        cameraData[32] = depth.width.toFloat()
        cameraData[33] = depth.height.toFloat()
        cameraData[34] = 0f
        cameraData[35] = 0f
        device.queue.writeBuffer(cameraBuffer, cameraData, offset = view.toLong() * cameraStride)
    }

    private fun nextFrameCheck() {
        val current = EngineStats.frame.value
        if (current == frame) return
        frame = current
        viewIndex = 0
        instanceOffset = 0
        retired.forEach { it.release() }
        retired.clear()
    }

    private fun grow(count: Int) {
        retired += instanceBuffer
        instanceCapacity = max(instanceCapacity * 2, count * 2)
        instanceBuffer = createInstanceBuffer(instanceCapacity)
        instanceOffset = 0
        cameraBindGroupDirty = true
    }

    private fun frameBindGroup(): BindGroup {
        val current = frameBindGroup
        if (current != null && !cameraBindGroupDirty) return current
        current?.release()
        cameraBindGroupDirty = false
        return device
            .createBindGroup(
                BindGroupDescriptor(
                    frameLayout,
                    listOf(
                        BindGroupEntry(
                            0,
                            BufferBinding(cameraBuffer, 0, CAMERA_FLOATS * Float.SIZE_BYTES.toLong()),
                        ),
                        BindGroupEntry(1, BufferBinding(instanceBuffer)),
                    ),
                    label = "DecalRenderer frame bind group",
                )
            )
            .also { frameBindGroup = it }
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
                    label = "DecalRenderer depth bind group",
                )
            )
            .also { depthBindGroup = it }
    }

    private fun textureBindGroup(texture: Texture): BindGroup =
        textureBindGroups[texture.id]
            ?: device
                .createBindGroup(
                    BindGroupDescriptor(
                        textureLayout,
                        listOf(BindGroupEntry(0, texture.view), BindGroupEntry(1, texture.sampler)),
                        label = "DecalRenderer texture bind group",
                    )
                )
                .also { textureBindGroups[texture.id] = it }

    private fun pipeline(sampleCount: Int): RenderPipeline {
        for (i in pipelines.indices) {
            val entry = pipelines[i]
            if (
                entry.blend == blendState &&
                    entry.sampleCount == sampleCount &&
                    entry.colorFormat == colorFormat &&
                    entry.depthFormat == depthFormat
            ) {
                return entry.pipeline
            }
        }
        val pipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = if (sampleCount > 1) msaaPipelineLayout else pipelineLayout,
                    vertex = VertexState(if (sampleCount > 1) msaaShader else shader, "vs_main"),
                    fragment =
                        FragmentState(
                            if (sampleCount > 1) msaaShader else shader,
                            "fs_main",
                            listOf(ColorTargetState(colorFormat, blendState)),
                        ),
                    primitive = PrimitiveState(cullMode = CullMode.FRONT),
                    depthStencil =
                        DepthStencilState(
                            format = depthFormat,
                            depthWriteEnabled = false,
                            depthCompare = CompareFunction.ALWAYS,
                        ),
                    multisample = MultisampleState(count = sampleCount),
                    label = "DecalRenderer pipeline",
                )
            )
        pipelines += PipelineEntry(blendState, colorFormat, depthFormat, sampleCount, pipeline)
        return pipeline
    }

    private fun createCameraBuffer(): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "DecalRenderer camera",
                maxViewsPerFrame.toLong() * cameraStride,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createInstanceBuffer(count: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "DecalRenderer instances",
                count.toLong() * INSTANCE_FLOATS * Float.SIZE_BYTES,
                BufferUsage.STORAGE or BufferUsage.COPY_DST,
                false,
            )
        )

    override fun release() {
        if (isReleased) return
        isReleased = true
        pipelines.forEach { it.pipeline.release() }
        pipelines.clear()
        textureBindGroups.forEachValue { it.release() }
        textureBindGroups.clear()
        depthBindGroup?.release()
        frameBindGroup?.release()
        retired.forEach { it.release() }
        retired.clear()
        cameraBuffer.release()
        instanceBuffer.release()
        pipelineLayout.release()
        msaaPipelineLayout.release()
        shader.release()
        msaaShader.release()
        frameLayout.release()
        depthLayout.release()
        msaaDepthLayout.release()
        textureLayout.release()
    }

    private class PipelineEntry(
        val blend: BlendState,
        val colorFormat: TextureFormat,
        val depthFormat: TextureFormat,
        val sampleCount: Int,
        val pipeline: RenderPipeline,
    )

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
                "DecalRenderer depth layout",
            )
        )

    private companion object {
        const val CAMERA_FLOATS = 36
        const val INSTANCE_FLOATS = 48
        val logger = Logger<DecalRenderer>()

        fun wgsl(multisampled: Boolean): String =
            WGSL.replace(
                    "texture_depth_2d",
                    if (multisampled) "texture_depth_multisampled_2d" else "texture_depth_2d",
                )

        const val WGSL =
            """
struct Camera {
    view_proj: mat4x4f,
    inv_view_proj: mat4x4f,
    viewport: vec4f,
};

struct Decal {
    model: mat4x4f,
    inv_model: mat4x4f,
    axis: vec4f,
    color: vec4f,
    uv: vec4f,
    params: vec4f,
};

@group(0) @binding(0) var<uniform> camera: Camera;
@group(0) @binding(1) var<storage, read> decals: array<Decal>;
@group(1) @binding(0) var depth_texture: texture_depth_2d;
@group(2) @binding(0) var decal_texture: texture_2d<f32>;
@group(2) @binding(1) var decal_sampler: sampler;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) @interpolate(flat) instance: u32,
};

@vertex
fn vs_main(@builtin(vertex_index) vertex: u32, @builtin(instance_index) instance: u32) -> VertexOutput {
    var corners = array<vec3f, 8>(
        vec3f(-0.5, -0.5, -0.5), vec3f(0.5, -0.5, -0.5), vec3f(0.5, 0.5, -0.5), vec3f(-0.5, 0.5, -0.5),
        vec3f(-0.5, -0.5, 0.5), vec3f(0.5, -0.5, 0.5), vec3f(0.5, 0.5, 0.5), vec3f(-0.5, 0.5, 0.5)
    );
    var faces = array<u32, 36>(
        0u, 2u, 1u, 0u, 3u, 2u,
        4u, 5u, 6u, 4u, 6u, 7u,
        0u, 1u, 5u, 0u, 5u, 4u,
        3u, 7u, 6u, 3u, 6u, 2u,
        0u, 4u, 7u, 0u, 7u, 3u,
        1u, 2u, 6u, 1u, 6u, 5u
    );
    let decal = decals[instance];
    var output: VertexOutput;
    output.position = camera.view_proj * decal.model * vec4f(corners[faces[vertex]], 1.0);
    output.instance = instance;
    return output;
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4f {
    let decal = decals[input.instance];
    let pixel = vec2i(input.position.xy);
    let depth = textureLoad(depth_texture, pixel, 0);
    let ndc = vec4f(
        input.position.x / camera.viewport.x * 2.0 - 1.0,
        1.0 - input.position.y / camera.viewport.y * 2.0,
        depth,
        1.0
    );
    let world_h = camera.inv_view_proj * ndc;
    let world = world_h.xyz / world_h.w;
    let normal = normalize(cross(dpdy(world), dpdx(world)));
    let local = (decal.inv_model * vec4f(world, 1.0)).xyz;
    if (any(abs(local) > vec3f(0.5))) {
        discard;
    }
    let facing = abs(dot(normal, normalize(decal.axis.xyz)));
    let angle_fade = smoothstep(decal.params.x, min(decal.params.x + 0.15, 1.0), facing);
    let edge = decal.params.y;
    let edge_fade = select(1.0, 1.0 - smoothstep(0.5 - edge, 0.5, abs(local.y)), edge > 0.0);
    let uv = vec2f(mix(decal.uv.x, decal.uv.z, local.x + 0.5), mix(decal.uv.y, decal.uv.w, local.z + 0.5));
    let color = textureSampleLevel(decal_texture, decal_sampler, uv, 0.0) * decal.color;
    let alpha = color.a * angle_fade * edge_fade;
    if (alpha <= 0.002) {
        discard;
    }
    return vec4f(color.rgb, alpha);
}
"""
    }
}
