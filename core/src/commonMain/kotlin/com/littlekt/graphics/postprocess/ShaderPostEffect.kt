package com.littlekt.graphics.postprocess

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*

/**
 * A [PostEffect] written as a WGSL function. [fragment] must define `fn effect(uv: vec2f) -> vec4f`
 * that returns the linear color at `uv`, and can use:
 * - `source` & `source_sampler`, the image so far, sampled with linear filtering & clamped edges
 * - `post.resolution` & `post.texel`, the size of `source` & the size of one pixel in uv
 * - `post.params[i]`, [paramCount] floats packed into `vec4f`s and set with [setParam]
 * - `post_texture_0` up to `post_texture_<textureCount - 1>`, [textureCount] more textures set
 *   with [setTexture] and sampled with `source_sampler`
 *
 * The result is encoded to sRGB when drawn into a target that isn't sRGB or float.
 *
 * ```
 * val invert = ShaderPostEffect(device, """
 *     fn effect(uv: vec2f) -> vec4f {
 *         let color = textureSample(source, source_sampler, uv);
 *         return vec4f(mix(color.rgb, 1.0 - color.rgb, post.params[0].x), color.a);
 *     }
 * """, paramCount = 1)
 * invert.setParam(0, 1f)
 * ```
 *
 * @param fragment the WGSL that defines `effect`
 * @param paramCount the number of floats in `post.params`
 * @param label the label of the GPU objects
 * @param textureCount the number of extra textures the effect samples, such as a lookup table
 * @author Colton Daily
 * @date 11/21/2026
 */
open class ShaderPostEffect(
    val device: Device,
    fragment: String,
    val paramCount: Int = 0,
    val label: String = "post effect",
    val textureCount: Int = 0,
) : PostEffect {

    init {
        require(paramCount >= 0) { "paramCount must not be negative!" }
        require(textureCount >= 0) { "textureCount must not be negative!" }
    }

    private val textures = arrayOfNulls<TextureView>(textureCount)

    override var enabled: Boolean = true

    private val paramVectors = maxOf(1, (paramCount + 3) / 4)
    private val data = FloatBuffer(HEADER_FLOATS + paramVectors * 4)
    private var dataDirty = true
    private var dataWidth = -1
    private var dataHeight = -1
    private var dataEncode = -1f

    private val shader =
        device.createShaderModule(
            """
            struct PostUniforms {
                resolution: vec2f,
                texel: vec2f,
                encode_srgb: f32,
                padding0: f32,
                padding1: f32,
                padding2: f32,
                params: array<vec4f, $paramVectors>,
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) uv: vec2f,
            };

            @group(0) @binding(0) var source: texture_2d<f32>;
            @group(0) @binding(1) var source_sampler: sampler;
            @group(0) @binding(2) var<uniform> post: PostUniforms;
            ${(0 until textureCount).joinToString("\n") { "@group(0) @binding(${it + 3}) var post_texture_$it: texture_2d<f32>;" }}

            @vertex
            fn vs_main(@builtin(vertex_index) index: u32) -> VertexOutput {
                let xy = vec2f(f32((index << 1u) & 2u), f32(index & 2u));
                var output: VertexOutput;
                output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
                output.uv = vec2f(xy.x, 1.0 - xy.y);
                return output;
            }

            fn post_linear_to_srgb(color: vec3f) -> vec3f {
                let c = clamp(color, vec3f(0.0), vec3f(1.0));
                let low = c * 12.92;
                let high = 1.055 * pow(c, vec3f(1.0 / 2.4)) - 0.055;
                return select(high, low, c <= vec3f(0.0031308));
            }

            $fragment

            @fragment
            fn fs_main(input: VertexOutput) -> @location(0) vec4f {
                var color = effect(input.uv);
                if (post.encode_srgb > 0.5) {
                    color = vec4f(post_linear_to_srgb(color.rgb), color.a);
                }
                return color;
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
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, BufferBindingLayout()),
                ) + List(textureCount) { BindGroupLayoutEntry(it + 3, ShaderStage.FRAGMENT, TextureBindingLayout()) },
                label = "$label bind group layout",
            )
        )

    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(bindGroupLayout), label = "$label pipeline layout")
        )

    private val sampler =
        device.createSampler(
            SamplerDescriptor(
                addressModeU = AddressMode.CLAMP_TO_EDGE,
                addressModeV = AddressMode.CLAMP_TO_EDGE,
                magFilter = FilterMode.LINEAR,
                minFilter = FilterMode.LINEAR,
            )
        )

    private val uniformBuffer =
        device.createGPUFloatBuffer("$label uniforms", data, BufferUsage.UNIFORM or BufferUsage.COPY_DST)
    private val uniformBinding = BufferBinding(uniformBuffer)

    private var pipeline: RenderPipeline? = null
    private var pipelineFormat: TextureFormat? = null
    private val bindGroups = arrayOfNulls<BindGroup>(BIND_GROUP_CACHE)
    private val bindGroupSources = arrayOfNulls<TextureView>(BIND_GROUP_CACHE)
    private var nextBindGroup = 0

    /**
     * Sets `post_texture_[index]` to [view], used from the next render. Every extra texture must be
     * set before the effect renders.
     */
    fun setTexture(index: Int, view: TextureView) {
        if (index < 0 || index >= textureCount) {
            throw IndexOutOfBoundsException("Texture $index is out of range of $textureCount textures!")
        }
        if (textures[index] === view) return
        textures[index] = view
        clearBindGroups()
    }

    /** @return the param at [index] of `post.params` */
    fun getParam(index: Int): Float {
        checkIndex(index)
        return data[HEADER_FLOATS + index]
    }

    /** Sets the param at [index] of `post.params`, uploaded on the next render. */
    fun setParam(index: Int, value: Float) {
        checkIndex(index)
        if (data[HEADER_FLOATS + index] == value) return
        data[HEADER_FLOATS + index] = value
        dataDirty = true
    }

    override fun render(
        renderPassEncoder: RenderPassEncoder,
        source: TextureView,
        format: TextureFormat,
        width: Int,
        height: Int,
    ) {
        val encode = if (format.srgb || format.hdr) 0f else 1f
        if (width != dataWidth || height != dataHeight || encode != dataEncode) {
            data[0] = width.toFloat()
            data[1] = height.toFloat()
            data[2] = 1f / width
            data[3] = 1f / height
            data[4] = encode
            dataWidth = width
            dataHeight = height
            dataEncode = encode
            dataDirty = true
        }
        if (dataDirty) {
            device.queue.writeBuffer(uniformBuffer, data)
            dataDirty = false
        }
        renderPassEncoder.setPipeline(getPipeline(format))
        renderPassEncoder.setBindGroup(0, getBindGroup(source))
        renderPassEncoder.draw(3, 1)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= paramCount) {
            throw IndexOutOfBoundsException("Param $index is out of range of $paramCount params!")
        }
    }

    private fun getPipeline(format: TextureFormat): RenderPipeline {
        val current = pipeline
        if (current != null && pipelineFormat == format) return current
        current?.release()
        pipelineFormat = format
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
                                    blendState = null,
                                    writeMask = ColorWriteMask.ALL,
                                ),
                        ),
                    primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                    label = "$label pipeline",
                )
            )
            .also { pipeline = it }
    }

    private fun getBindGroup(source: TextureView): BindGroup {
        for (i in 0 until BIND_GROUP_CACHE) {
            if (bindGroupSources[i] === source) return bindGroups[i]!!
        }
        val slot = nextBindGroup
        nextBindGroup = (nextBindGroup + 1) % BIND_GROUP_CACHE
        bindGroups[slot]?.release()
        bindGroupSources[slot] = source
        val entries =
            mutableListOf(
                BindGroupEntry(0, source),
                BindGroupEntry(1, sampler),
                BindGroupEntry(2, uniformBinding),
            )
        for (i in 0 until textureCount) {
            val view = textures[i] ?: throw IllegalStateException("$label needs post_texture_$i set with setTexture before it renders!")
            entries += BindGroupEntry(i + 3, view)
        }
        return device
            .createBindGroup(BindGroupDescriptor(bindGroupLayout, entries, label = "$label bind group"))
            .also { bindGroups[slot] = it }
    }

    private fun clearBindGroups() {
        for (i in 0 until BIND_GROUP_CACHE) {
            bindGroups[i]?.release()
            bindGroups[i] = null
            bindGroupSources[i] = null
        }
    }

    override fun release() {
        clearBindGroups()
        pipeline?.release()
        pipeline = null
        uniformBuffer.release()
        sampler.release()
        pipelineLayout.release()
        bindGroupLayout.release()
        shader.release()
    }

    private companion object {
        const val HEADER_FLOATS = 8
        const val BIND_GROUP_CACHE = 3
    }
}
