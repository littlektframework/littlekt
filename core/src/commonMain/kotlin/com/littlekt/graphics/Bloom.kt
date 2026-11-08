package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * Adds a glow around the bright parts of an HDR texture, such as a
 * [TextureFormat.RGBA16_FLOAT] render target. Colors brighter than the [threshold] are blurred
 * through a chain of smaller textures and added back on top of the source texture. Render it after
 * the scene and before the [Tonemapper].
 *
 * Usage each frame:
 * ```
 * modelBatch.flush(scenePass, camera, dt)
 * scenePass.end()
 * bloom.render(commandEncoder, hdrView, width, height)
 * tonemapper.render(tonemapPass, hdrView)
 * ```
 *
 * @param format the [TextureFormat] of the source texture. Must be a float format.
 * @param mipLevels the max number of textures in the blur chain. More levels give a wider glow.
 * @author Colton Daily
 * @date 11/8/2026
 */
class Bloom(
    val device: Device,
    val format: TextureFormat = TextureFormat.RGBA16_FLOAT,
    val mipLevels: Int = 6,
) : Releasable {

    init {
        require(format.hdr) { "Bloom.format must be a float format!" }
        require(mipLevels >= 1) { "Bloom.mipLevels must be at least 1!" }
    }

    /** Colors brighter than this, in linear HDR, start to glow. */
    var threshold: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    /** Softens the [threshold] so colors fade into the glow. `0` is a hard cut off. */
    var softKnee: Float = 0.5f
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    /** How much of the glow is added back to the source texture. */
    var intensity: Float = 0.3f
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    /** The spread of each blur step, in texels. */
    var radius: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    private val shader =
        device.createShaderModule(
            """
            struct Params {
                threshold: f32,
                knee: f32,
                intensity: f32,
                radius: f32,
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) uv: vec2f,
            };

            @group(0) @binding(0) var source: texture_2d<f32>;
            @group(0) @binding(1) var source_sampler: sampler;
            @group(0) @binding(2) var<uniform> params: Params;

            @vertex
            fn vs_main(@builtin(vertex_index) index: u32) -> VertexOutput {
                let xy = vec2f(f32((index << 1u) & 2u), f32(index & 2u));
                var output: VertexOutput;
                output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
                output.uv = vec2f(xy.x, 1.0 - xy.y);
                return output;
            }

            fn sample(uv: vec2f, offset: vec2f) -> vec3f {
                return textureSampleLevel(source, source_sampler, uv + offset, 0.0).rgb;
            }

            fn luma(color: vec3f) -> f32 {
                return dot(color, vec3f(0.2126, 0.7152, 0.0722));
            }

            fn karis(color: vec3f) -> f32 {
                return 1.0 / (1.0 + luma(color));
            }

            @fragment
            fn fs_prefilter(input: VertexOutput) -> @location(0) vec4f {
                let t = 1.0 / vec2f(textureDimensions(source));
                let uv = input.uv;
                let a = sample(uv, t * vec2f(-2.0, 2.0));
                let b = sample(uv, t * vec2f(0.0, 2.0));
                let c = sample(uv, t * vec2f(2.0, 2.0));
                let d = sample(uv, t * vec2f(-2.0, 0.0));
                let e = sample(uv, vec2f(0.0));
                let f = sample(uv, t * vec2f(2.0, 0.0));
                let g = sample(uv, t * vec2f(-2.0, -2.0));
                let h = sample(uv, t * vec2f(0.0, -2.0));
                let i = sample(uv, t * vec2f(2.0, -2.0));
                let j = sample(uv, t * vec2f(-1.0, 1.0));
                let k = sample(uv, t * vec2f(1.0, 1.0));
                let l = sample(uv, t * vec2f(-1.0, -1.0));
                let m = sample(uv, t * vec2f(1.0, -1.0));
                let g0 = (j + k + l + m) * 0.25;
                let g1 = (a + b + d + e) * 0.25;
                let g2 = (b + c + e + f) * 0.25;
                let g3 = (d + e + g + h) * 0.25;
                let g4 = (e + f + h + i) * 0.25;
                let w0 = karis(g0) * 0.5;
                let w1 = karis(g1) * 0.125;
                let w2 = karis(g2) * 0.125;
                let w3 = karis(g3) * 0.125;
                let w4 = karis(g4) * 0.125;
                let color = (g0 * w0 + g1 * w1 + g2 * w2 + g3 * w3 + g4 * w4) / (w0 + w1 + w2 + w3 + w4);
                let brightness = max(color.r, max(color.g, color.b));
                var soft = clamp(brightness - params.threshold + params.knee, 0.0, 2.0 * params.knee);
                soft = soft * soft / (4.0 * params.knee + 0.00001);
                let contribution = max(soft, brightness - params.threshold) / max(brightness, 0.00001);
                return vec4f(color * contribution, 1.0);
            }

            @fragment
            fn fs_downsample(input: VertexOutput) -> @location(0) vec4f {
                let t = 1.0 / vec2f(textureDimensions(source));
                let uv = input.uv;
                let a = sample(uv, t * vec2f(-2.0, 2.0));
                let b = sample(uv, t * vec2f(0.0, 2.0));
                let c = sample(uv, t * vec2f(2.0, 2.0));
                let d = sample(uv, t * vec2f(-2.0, 0.0));
                let e = sample(uv, vec2f(0.0));
                let f = sample(uv, t * vec2f(2.0, 0.0));
                let g = sample(uv, t * vec2f(-2.0, -2.0));
                let h = sample(uv, t * vec2f(0.0, -2.0));
                let i = sample(uv, t * vec2f(2.0, -2.0));
                let j = sample(uv, t * vec2f(-1.0, 1.0));
                let k = sample(uv, t * vec2f(1.0, 1.0));
                let l = sample(uv, t * vec2f(-1.0, -1.0));
                let m = sample(uv, t * vec2f(1.0, -1.0));
                let color = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
                return vec4f(color, 1.0);
            }

            fn tent(uv: vec2f) -> vec3f {
                let t = params.radius / vec2f(textureDimensions(source));
                let a = sample(uv, t * vec2f(-1.0, 1.0));
                let b = sample(uv, t * vec2f(0.0, 1.0));
                let c = sample(uv, t * vec2f(1.0, 1.0));
                let d = sample(uv, t * vec2f(-1.0, 0.0));
                let e = sample(uv, vec2f(0.0));
                let f = sample(uv, t * vec2f(1.0, 0.0));
                let g = sample(uv, t * vec2f(-1.0, -1.0));
                let h = sample(uv, t * vec2f(0.0, -1.0));
                let i = sample(uv, t * vec2f(1.0, -1.0));
                return (e * 4.0 + (b + d + f + h) * 2.0 + (a + c + g + i)) / 16.0;
            }

            @fragment
            fn fs_upsample(input: VertexOutput) -> @location(0) vec4f {
                return vec4f(tent(input.uv), 1.0);
            }

            @fragment
            fn fs_composite(input: VertexOutput) -> @location(0) vec4f {
                return vec4f(tent(input.uv) * params.intensity, 0.0);
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
                ),
                label = "bloom bind group layout",
            )
        )

    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayout, label = "bloom pipeline layout")
        )

    private val sampler =
        device.createSampler(
            SamplerDescriptor(
                magFilter = FilterMode.LINEAR,
                minFilter = FilterMode.LINEAR,
                label = "bloom sampler",
            )
        )

    private val paramsData = FloatBuffer(4)
    private val paramsBuffer =
        device.createGPUFloatBuffer(
            "bloom params",
            paramsData,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    private val paramsBinding = BufferBinding(paramsBuffer)
    private var paramsDirty = true

    private val additive =
        BlendState(
            color = BlendComponent(BlendFactor.ONE, BlendFactor.ONE),
            alpha = BlendComponent(BlendFactor.ZERO, BlendFactor.ONE),
        )
    private val prefilterPipeline = createPipeline("fs_prefilter", null)
    private val downsamplePipeline = createPipeline("fs_downsample", null)
    private val upsamplePipeline = createPipeline("fs_upsample", additive)
    private val compositePipeline = createPipeline("fs_composite", additive)

    private var chainWidth = 0
    private var chainHeight = 0
    private var chainLevels = 0
    private var chainTexture: WebGPUTexture? = null
    private val chainViews = mutableListOf<TextureView>()
    private val chainBindGroups = mutableListOf<BindGroup>()
    private val downPasses = mutableListOf<RenderPassDescriptor>()
    private val upPasses = mutableListOf<RenderPassDescriptor>()

    private var source: TextureView? = null
    private var sourceBindGroup: BindGroup? = null
    private var compositePass: RenderPassDescriptor? = null

    /**
     * Blurs the bright parts of the [source] texture view and adds them back on top of it. The
     * [source] must be a [format] texture of [width] & [height] with
     * [TextureUsage.RENDER_ATTACHMENT] & [TextureUsage.TEXTURE] usages and must not be
     * multisampled.
     */
    fun render(commandEncoder: CommandEncoder, source: TextureView, width: Int, height: Int) {
        if (paramsDirty) {
            paramsData[0] = threshold
            paramsData[1] = threshold * softKnee
            paramsData[2] = intensity
            paramsData[3] = radius
            device.queue.writeBuffer(paramsBuffer, paramsData)
            paramsDirty = false
        }
        ensureChain(max(1, width / 2), max(1, height / 2))
        ensureSource(source)

        draw(commandEncoder, downPasses[0], prefilterPipeline, sourceBindGroup!!)
        for (i in 1 until chainLevels) {
            draw(commandEncoder, downPasses[i], downsamplePipeline, chainBindGroups[i - 1])
        }
        for (i in chainLevels - 1 downTo 1) {
            draw(commandEncoder, upPasses[i - 1], upsamplePipeline, chainBindGroups[i])
        }
        draw(commandEncoder, compositePass!!, compositePipeline, chainBindGroups[0])
    }

    private fun draw(
        commandEncoder: CommandEncoder,
        descriptor: RenderPassDescriptor,
        pipeline: RenderPipeline,
        bindGroup: BindGroup,
    ) {
        val pass = commandEncoder.beginRenderPass(descriptor)
        pass.setPipeline(pipeline)
        pass.setBindGroup(0, bindGroup)
        pass.draw(3, 1)
        pass.end()
        pass.release()
    }

    private fun ensureChain(width: Int, height: Int) {
        if (chainTexture != null && chainWidth == width && chainHeight == height) return
        releaseChain()
        chainWidth = width
        chainHeight = height
        chainLevels = min(mipLevels, log2(min(width, height).toFloat()).toInt() + 1)
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(width, height, 1),
                    chainLevels,
                    1,
                    TextureDimension.D2,
                    format,
                    TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                    "bloom chain",
                )
            )
        chainTexture = texture
        for (i in 0 until chainLevels) {
            val view =
                texture.createView(
                    TextureViewDescriptor(format, TextureViewDimension.D2, baseMipLevel = i)
                )
            chainViews += view
            chainBindGroups += createBindGroup(view)
            downPasses += passDescriptor(view, LoadOp.CLEAR, "bloom downsample pass $i")
        }
        for (i in 0 until chainLevels - 1) {
            upPasses += passDescriptor(chainViews[i], LoadOp.LOAD, "bloom upsample pass $i")
        }
    }

    private fun ensureSource(view: TextureView) {
        if (source === view && sourceBindGroup != null) return
        sourceBindGroup?.release()
        source = view
        sourceBindGroup = createBindGroup(view)
        compositePass = passDescriptor(view, LoadOp.LOAD, "bloom composite pass")
    }

    private fun createBindGroup(view: TextureView) =
        device.createBindGroup(
            BindGroupDescriptor(
                bindGroupLayout,
                listOf(
                    BindGroupEntry(0, view),
                    BindGroupEntry(1, sampler),
                    BindGroupEntry(2, paramsBinding),
                ),
                label = "bloom bind group",
            )
        )

    private fun passDescriptor(view: TextureView, loadOp: LoadOp, label: String) =
        RenderPassDescriptor(
            listOf(
                RenderPassColorAttachmentDescriptor(
                    view = view,
                    loadOp = loadOp,
                    storeOp = StoreOp.STORE,
                    clearColor = Color.BLACK,
                )
            ),
            label = label,
        )

    private fun createPipeline(entryPoint: String, blendState: BlendState?) =
        device.createRenderPipeline(
            RenderPipelineDescriptor(
                pipelineLayout,
                vertex = VertexState(module = shader, entryPoint = "vs_main"),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = entryPoint,
                        target =
                            ColorTargetState(
                                format = format,
                                blendState = blendState,
                                writeMask = ColorWriteMask.ALL,
                            ),
                    ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                label = "bloom $entryPoint pipeline",
            )
        )

    private fun releaseChain() {
        chainBindGroups.forEach { it.release() }
        chainBindGroups.clear()
        chainViews.forEach { it.release() }
        chainViews.clear()
        chainTexture?.release()
        chainTexture = null
        downPasses.clear()
        upPasses.clear()
    }

    override fun release() {
        releaseChain()
        sourceBindGroup?.release()
        sourceBindGroup = null
        source = null
        compositePass = null
        prefilterPipeline.release()
        downsamplePipeline.release()
        upsamplePipeline.release()
        compositePipeline.release()
        paramsBuffer.release()
        sampler.release()
        pipelineLayout.release()
        bindGroupLayout.release()
        shader.release()
    }
}
