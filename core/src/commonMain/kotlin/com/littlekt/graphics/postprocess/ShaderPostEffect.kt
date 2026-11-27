package com.littlekt.graphics.postprocess

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.*

/**
 * A [PostEffect] written as a WGSL function. [fragment] must define `fn effect(uv: vec2f) -> vec4f`
 * that returns the linear color at `uv`, and can use:
 * - `source` & `source_sampler`, the image so far, sampled with linear filtering & clamped edges
 * - `post.resolution` & `post.texel`, the size of `source` & the size of one pixel in uv
 * - `post.params[i]`, [paramCount] floats packed into `vec4f`s and set with [setParam]
 * - `post_texture_0` up to `post_texture_<textureCount - 1>`, [textureCount] more textures set
 *   with [setTexture] and sampled with `source_sampler`
 * - `post_depth_0` up to `post_depth_<depthTextureCount - 1>`, [depthTextureCount] depth textures
 *   set with [setDepthTexture], read with `post_load_depth_<i>(uv)`
 * - `post_linear_depth(depth)`, the distance from the camera of a depth, using `post.near` &
 *   `post.far` set with [setCamera] or [setDepthRange]
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
 * @param depthTextureCount the number of depth textures the effect reads, such as the scene depth
 *   for fog
 * @param depthSampleCount the sample count of the depth textures. A multisampled depth texture is
 *   read from its first sample.
 * @author Colton Daily
 * @date 11/21/2026
 */
open class ShaderPostEffect(
    val device: Device,
    fragment: String,
    val paramCount: Int = 0,
    val label: String = "post effect",
    val textureCount: Int = 0,
    val depthTextureCount: Int = 0,
    val depthSampleCount: Int = 1,
) : PostEffect {

    init {
        require(paramCount >= 0) { "paramCount must not be negative!" }
        require(textureCount >= 0) { "textureCount must not be negative!" }
        require(depthTextureCount >= 0) { "depthTextureCount must not be negative!" }
        require(depthSampleCount >= 1) { "depthSampleCount must be at least 1!" }
    }

    private val textures = arrayOfNulls<TextureView>(textureCount)
    private val depthTextures = arrayOfNulls<TextureView>(depthTextureCount)
    private val depthBinding = 3 + textureCount

    override var enabled: Boolean = true

    private val paramVectors = maxOf(1, (paramCount + 3) / 4)
    private val data =
        FloatBuffer(HEADER_FLOATS + paramVectors * 4).apply {
            this[5] = 0.01f
            this[6] = 1000f
        }
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
                near: f32,
                far: f32,
                padding: f32,
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
            ${(0 until depthTextureCount).joinToString("\n") { depthSrc(it) }}

            fn post_linear_depth(depth: f32) -> f32 {
                return post.near * post.far / (post.far - depth * (post.far - post.near));
            }

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
                ) +
                    List(textureCount) {
                        BindGroupLayoutEntry(it + 3, ShaderStage.FRAGMENT, TextureBindingLayout())
                    } +
                    List(depthTextureCount) {
                        BindGroupLayoutEntry(
                            depthBinding + it,
                            ShaderStage.FRAGMENT,
                            TextureBindingLayout(
                                TextureSampleType.DEPTH,
                                multisampled = depthSampleCount > 1,
                            ),
                        )
                    },
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

    /**
     * Sets `post_depth_[index]` to [view], used from the next render. The view must only have the
     * depth aspect, such as [com.littlekt.graphics.EmptyTexture.depthView]. Every depth texture must
     * be set before the effect renders.
     */
    fun setDepthTexture(index: Int, view: TextureView) {
        if (index < 0 || index >= depthTextureCount) {
            throw IndexOutOfBoundsException(
                "Depth texture $index is out of range of $depthTextureCount depth textures!"
            )
        }
        if (depthTextures[index] === view) return
        depthTextures[index] = view
        clearBindGroups()
    }

    /** Sets `post.near` & `post.far` used by `post_linear_depth`, uploaded on the next render. */
    fun setDepthRange(near: Float, far: Float) {
        if (data[5] == near && data[6] == far) return
        data[5] = near
        data[6] = far
        dataDirty = true
    }

    /** Sets `post.near` & `post.far` to the near & far planes of the [camera]. */
    fun setCamera(camera: Camera) = setDepthRange(camera.near, camera.far)

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
        for (i in 0 until depthTextureCount) {
            val view = depthTextures[i] ?: throw IllegalStateException("$label needs post_depth_$i set with setDepthTexture before it renders!")
            entries += BindGroupEntry(depthBinding + i, view)
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

    private fun depthSrc(index: Int): String {
        val type = if (depthSampleCount > 1) "texture_depth_multisampled_2d" else "texture_depth_2d"
        return """
            @group(0) @binding(${depthBinding + index}) var post_depth_$index: $type;

            fn post_load_depth_$index(uv: vec2f) -> f32 {
                let size = vec2i(textureDimensions(post_depth_$index));
                let coord = clamp(vec2i(uv * vec2f(size)), vec2i(0), size - vec2i(1));
                return textureLoad(post_depth_$index, coord, 0);
            }
        """
            .trimIndent()
    }

    private companion object {
        const val HEADER_FLOATS = 8
        const val BIND_GROUP_CACHE = 3
    }
}
