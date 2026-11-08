package com.littlekt.graphics.g3d.ibl

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4

/**
 * Draws the [EnvironmentMap.cubeView] of an [environmentMap] behind everything else. Render it in
 * the same render pass, after the scene, so only the pixels not covered by the scene are drawn.
 * It may be rendered multiple times per frame with different cameras, up to [MAX_VIEWS_PER_FRAME].
 *
 * Usage each frame:
 * ```
 * modelBatch.flush(renderPass, camera, dt)
 * skybox.render(renderPass, camera)
 * ```
 *
 * @param colorFormat the [TextureFormat] of the color attachment.
 * @param depthFormat the [TextureFormat] of the depth attachment, or `null` if there isn't one.
 * @param sampleCount the number of samples per pixel of the attachments for multisampling.
 * @author Colton Daily
 * @date 11/8/2026
 */
class Skybox(
    val device: Device,
    val environmentMap: EnvironmentMap,
    colorFormat: TextureFormat,
    depthFormat: TextureFormat? = TextureFormat.DEPTH24_PLUS_STENCIL8,
    sampleCount: Int = 1,
) : Releasable {

    /** The [TextureFormat] of the color attachment. */
    var colorFormat: TextureFormat = colorFormat
        set(value) {
            if (field == value) return
            field = value
            pipelineDirty = true
        }

    /** The [TextureFormat] of the depth attachment, or `null` if there isn't one. */
    var depthFormat: TextureFormat? = depthFormat
        set(value) {
            if (field == value) return
            field = value
            pipelineDirty = true
        }

    /** The number of samples per pixel of the attachments for multisampling. */
    var sampleCount: Int = sampleCount
        set(value) {
            if (field == value) return
            field = value
            pipelineDirty = true
        }

    /** The mip level of the environment cubemap to draw. Higher values blur the sky. */
    var lod: Float = 0f

    /** Multiplier of the sky color. */
    var intensity: Float = 1f

    private val alignment = device.limits.minUniformBufferOffsetAlignment
    private val uniformData = FloatBuffer(UNIFORM_SIZE)
    private val uniformBuffer =
        device.createBuffer(
            BufferDescriptor(
                "skybox uniform",
                alignment.toLong() * MAX_VIEWS_PER_FRAME,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )
    private val dynamicOffsets = List(MAX_VIEWS_PER_FRAME) { listOf(it * alignment.toLong()) }

    private val shader =
        device.createShaderModule(
            """
            struct Sky {
                inv_view_proj: mat4x4f,
                lod: f32,
                intensity: f32,
                gamma_correct: f32,
                padding: f32,
            };

            @group(0) @binding(0) var<uniform> sky: Sky;
            @group(0) @binding(1) var sky_texture: texture_cube<f32>;
            @group(0) @binding(2) var sky_sampler: sampler;

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) ndc: vec2f,
            };

            @vertex
            fn vs_main(@builtin(vertex_index) vertex: u32) -> VertexOutput {
                let xy = vec2f(f32((vertex << 1u) & 2u), f32(vertex & 2u)) * 2.0 - 1.0;
                var output: VertexOutput;
                output.position = vec4f(xy, 1.0, 1.0);
                output.ndc = xy;
                return output;
            }

            fn linear_to_srgb(color: vec3f) -> vec3f {
                let low = color * 12.92;
                let high = 1.055 * pow(color, vec3f(1.0 / 2.4)) - 0.055;
                return select(high, low, color <= vec3f(0.0031308));
            }

            @fragment
            fn fs_main(input: VertexOutput) -> @location(0) vec4f {
                let world = sky.inv_view_proj * vec4f(input.ndc, 1.0, 1.0);
                let dir = normalize(world.xyz / world.w);
                var color = textureSampleLevel(sky_texture, sky_sampler, dir, sky.lod).rgb * sky.intensity;
                if (sky.gamma_correct > 0.5) {
                    color = linear_to_srgb(clamp(color, vec3f(0.0), vec3f(1.0)));
                }
                return vec4f(color, 1.0);
            }
            """
                .trimIndent()
        )

    private val bindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.FRAGMENT,
                        BufferBindingLayout(hasDynamicOffset = true),
                    ),
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.FRAGMENT,
                        TextureBindingLayout(viewDimension = TextureViewDimension.CUBE),
                    ),
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                label = "skybox bind group layout",
            )
        )

    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayout, label = "skybox pipeline layout")
        )

    private val bindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                bindGroupLayout,
                listOf(
                    BindGroupEntry(0, BufferBinding(uniformBuffer, size = UNIFORM_SIZE * 4L)),
                    BindGroupEntry(1, environmentMap.cubeView),
                    BindGroupEntry(2, environmentMap.sampler),
                ),
                label = "skybox bind group",
            )
        )

    private var pipeline: RenderPipeline? = null
    private var pipelineDirty = true
    private var lastFrame = -1L
    private var slot = 0
    private val viewNoTranslation = Mat4()
    private val viewProjection = Mat4()
    private val invViewProjection = Mat4()

    /** Draws the sky into the current [renderPassEncoder] using the [camera]. */
    fun render(renderPassEncoder: RenderPassEncoder, camera: Camera) {
        val frame = EngineStats.frame.value
        if (frame != lastFrame) {
            lastFrame = frame
            slot = 0
        }
        val index = slot % MAX_VIEWS_PER_FRAME
        slot++

        viewNoTranslation.set(camera.view)
        viewNoTranslation.m03 = 0f
        viewNoTranslation.m13 = 0f
        viewNoTranslation.m23 = 0f
        camera.projection.mul(viewNoTranslation, viewProjection)
        viewProjection.invert(invViewProjection)

        uniformData.position = 0
        uniformData.put(invViewProjection.data)
        uniformData.put(lod)
        uniformData.put(intensity)
        uniformData.put(if (colorFormat.srgb || colorFormat.hdr) 0f else 1f)
        uniformData.put(0f)
        device.queue.writeBuffer(uniformBuffer, uniformData, offset = index * alignment.toLong())

        renderPassEncoder.setPipeline(getPipeline())
        renderPassEncoder.setBindGroup(0, bindGroup, dynamicOffsets[index])
        renderPassEncoder.draw(3, 1)
    }

    private fun getPipeline(): RenderPipeline {
        val current = pipeline
        if (current != null && !pipelineDirty) return current
        current?.release()
        pipelineDirty = false
        val depthFormat = depthFormat
        return device
            .createRenderPipeline(
                RenderPipelineDescriptor(
                    pipelineLayout,
                    vertex = VertexState(module = shader, entryPoint = "vs_main"),
                    fragment =
                        FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            target =
                                ColorTargetState(
                                    format = colorFormat,
                                    blendState = null,
                                    writeMask = ColorWriteMask.ALL,
                                ),
                        ),
                    primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                    depthStencil =
                        depthFormat?.let {
                            DepthStencilState(it, false, CompareFunction.LESS_EQUAL)
                        },
                    multisample = MultisampleState(count = sampleCount),
                    label = "skybox pipeline",
                )
            )
            .also { pipeline = it }
    }

    override fun release() {
        pipeline?.release()
        pipeline = null
        bindGroup.release()
        pipelineLayout.release()
        bindGroupLayout.release()
        shader.release()
        uniformBuffer.release()
    }

    companion object {
        /** The max number of times a [Skybox] may be rendered per frame. */
        const val MAX_VIEWS_PER_FRAME = 8

        private const val UNIFORM_SIZE = 20
    }
}
