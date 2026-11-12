package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*

/**
 * Draws a texture over the whole of the current render pass, scaling it with a chosen
 * [FilterMode]. Use it to upscale a lower resolution render target to the screen, copy a texture
 * into another of a different size or show a texture for debugging. Set a viewport on the render
 * pass to draw into part of it.
 *
 * ```
 * val renderPass = commandEncoder.beginRenderPass(surfaceRenderPassDescriptor)
 * blitter.blit(renderPass, sceneTarget.view, FilterMode.LINEAR)
 * renderPass.end()
 * ```
 *
 * @param format the [TextureFormat] of the render pass target this will be drawn to.
 * @author Colton Daily
 * @date 11/12/2026
 */
class TextureBlitter(val device: Device, format: TextureFormat) : Releasable {

    /**
     * The [TextureFormat] of the render pass target this will be drawn to. Changing this will
     * recreate the pipelines on the next [blit].
     */
    var format: TextureFormat = format
        set(value) {
            if (field == value) return
            field = value
            releasePipelines()
        }

    private val shader =
        device.createShaderModule(
            """
            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) uv: vec2f,
            };

            @group(0) @binding(0) var source: texture_2d<f32>;
            @group(0) @binding(1) var source_sampler: sampler;

            @vertex
            fn vs_main(@builtin(vertex_index) index: u32) -> VertexOutput {
                let xy = vec2f(f32((index << 1u) & 2u), f32(index & 2u));
                var output: VertexOutput;
                output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
                output.uv = vec2f(xy.x, 1.0 - xy.y);
                return output;
            }

            @fragment
            fn fs_main(input: VertexOutput) -> @location(0) vec4f {
                return textureSample(source, source_sampler, input.uv);
            }
            """
                .trimIndent()
        )

    private val bindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                label = "texture blitter bind group layout",
            )
        )

    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(bindGroupLayout),
                label = "texture blitter pipeline layout",
            )
        )

    private val linearSampler =
        device.createSampler(
            SamplerDescriptor(magFilter = FilterMode.LINEAR, minFilter = FilterMode.LINEAR)
        )
    private val nearestSampler =
        device.createSampler(
            SamplerDescriptor(magFilter = FilterMode.NEAREST, minFilter = FilterMode.NEAREST)
        )

    private var opaquePipeline: RenderPipeline? = null
    private var blendPipeline: RenderPipeline? = null
    private val bindGroups = arrayOfNulls<BindGroup>(2)
    private val bindGroupSources = arrayOfNulls<TextureView>(2)

    /**
     * Draws [source] over the whole of the current [renderPassEncoder]. The render pass target
     * must match [format].
     *
     * @param filter how the texture is sampled when its size differs from the target
     * @param blend `true` to alpha blend over what is already drawn instead of replacing it
     */
    fun blit(
        renderPassEncoder: RenderPassEncoder,
        source: TextureView,
        filter: FilterMode = FilterMode.LINEAR,
        blend: Boolean = false,
    ) {
        renderPassEncoder.setPipeline(getPipeline(blend))
        renderPassEncoder.setBindGroup(0, getBindGroup(source, filter))
        renderPassEncoder.draw(3, 1)
    }

    private fun getPipeline(blend: Boolean): RenderPipeline {
        val current = if (blend) blendPipeline else opaquePipeline
        if (current != null) return current
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
                                    format = format,
                                    blendState = if (blend) BlendState.NonPreMultiplied else null,
                                    writeMask = ColorWriteMask.ALL,
                                ),
                        ),
                    primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                    label = "texture blitter pipeline",
                )
            )
            .also { if (blend) blendPipeline = it else opaquePipeline = it }
    }

    private fun getBindGroup(source: TextureView, filter: FilterMode): BindGroup {
        val slot = if (filter == FilterMode.NEAREST) 1 else 0
        val current = bindGroups[slot]
        if (current != null && bindGroupSources[slot] === source) return current
        current?.release()
        bindGroupSources[slot] = source
        return device
            .createBindGroup(
                BindGroupDescriptor(
                    bindGroupLayout,
                    listOf(
                        BindGroupEntry(0, source),
                        BindGroupEntry(1, if (slot == 1) nearestSampler else linearSampler),
                    ),
                    label = "texture blitter bind group",
                )
            )
            .also { bindGroups[slot] = it }
    }

    private fun releasePipelines() {
        opaquePipeline?.release()
        opaquePipeline = null
        blendPipeline?.release()
        blendPipeline = null
    }

    override fun release() {
        for (i in bindGroups.indices) {
            bindGroups[i]?.release()
            bindGroups[i] = null
            bindGroupSources[i] = null
        }
        releasePipelines()
        linearSampler.release()
        nearestSampler.release()
        pipelineLayout.release()
        bindGroupLayout.release()
        shader.release()
    }
}
