package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*

/**
 * Maps an HDR texture, such as a [TextureFormat.RGBA16_FLOAT] render target, down to a displayable
 * range using a [TonemapMode] and draws it to the current render pass. If the [format] of the
 * target is not sRGB, the result will be gamma corrected in the shader.
 *
 * Usage each frame:
 * ```
 * val renderPass = commandEncoder.beginRenderPass(surfaceRenderPassDescriptor)
 * tonemapper.render(renderPass, hdrView)
 * renderPass.end()
 * ```
 *
 * @param format the [TextureFormat] of the render pass target this will be drawn to.
 * @author Colton Daily
 * @date 11/8/2026
 */
class Tonemapper(val device: Device, format: TextureFormat) : Releasable {

    /**
     * The [TextureFormat] of the render pass target this will be drawn to. Changing this will
     * recreate the pipeline on the next [render].
     */
    var format: TextureFormat = format
        set(value) {
            if (field == value) return
            field = value
            pipelineDirty = true
            paramsDirty = true
        }

    /** The [TonemapMode] used to map the HDR colors. */
    var mode: TonemapMode = TonemapMode.ACES
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    /** Multiplier applied to the HDR colors before tonemapping. */
    var exposure: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            paramsDirty = true
        }

    private val shader =
        device.createShaderModule(
            """
            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) uv: vec2f,
            };

            struct Params {
                exposure: f32,
                mode: f32,
                gamma_correct: f32,
                padding: f32,
            };

            @group(0) @binding(0) var hdr_texture: texture_2d<f32>;
            @group(0) @binding(1) var<uniform> params: Params;

            @vertex
            fn vs_main(@builtin(vertex_index) index: u32) -> VertexOutput {
                let xy = vec2f(f32((index << 1u) & 2u), f32(index & 2u));
                var output: VertexOutput;
                output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
                output.uv = vec2f(xy.x, 1.0 - xy.y);
                return output;
            }

            fn reinhard(color: vec3f) -> vec3f {
                return color / (1.0 + color);
            }

            fn aces(color: vec3f) -> vec3f {
                let input_mat = mat3x3f(
                    vec3f(0.59719, 0.07600, 0.02840),
                    vec3f(0.35458, 0.90834, 0.13383),
                    vec3f(0.04823, 0.01566, 0.83777),
                );
                let output_mat = mat3x3f(
                    vec3f(1.60475, -0.10208, -0.00327),
                    vec3f(-0.53108, 1.10813, -0.07276),
                    vec3f(-0.07367, -0.00605, 1.07602),
                );
                let v = input_mat * color;
                let a = v * (v + 0.0245786) - 0.000090537;
                let b = v * (0.983729 * v + 0.4329510) + 0.238081;
                return clamp(output_mat * (a / b), vec3f(0.0), vec3f(1.0));
            }

            fn agx_contrast(x: vec3f) -> vec3f {
                let x2 = x * x;
                let x4 = x2 * x2;
                return 15.5 * x4 * x2 - 40.14 * x4 * x + 31.96 * x4 - 6.868 * x2 * x + 0.4298 * x2 + 0.1191 * x - 0.00232;
            }

            fn agx(color: vec3f) -> vec3f {
                let agx_mat = mat3x3f(
                    vec3f(0.842479062253094, 0.0423282422610123, 0.0423756549057051),
                    vec3f(0.0784335999999992, 0.878468636469772, 0.0784336),
                    vec3f(0.0792237451477643, 0.0791661274605434, 0.879142973793104),
                );
                let agx_mat_inv = mat3x3f(
                    vec3f(1.19687900512017, -0.0528968517574562, -0.0529716355144438),
                    vec3f(-0.0980208811401368, 1.15190312990417, -0.0980434501171241),
                    vec3f(-0.0990297440797205, -0.0989611768448433, 1.15107367264116),
                );
                let min_ev = -12.47393;
                let max_ev = 4.026069;
                var v = agx_mat * color;
                v = clamp(log2(max(v, vec3f(1e-10))), vec3f(min_ev), vec3f(max_ev));
                v = (v - min_ev) / (max_ev - min_ev);
                v = agx_contrast(v);
                v = agx_mat_inv * v;
                return pow(clamp(v, vec3f(0.0), vec3f(1.0)), vec3f(2.2));
            }

            fn neutral(color: vec3f) -> vec3f {
                let start_compression = 0.8 - 0.04;
                let desaturation = 0.15;
                let x = min(color.r, min(color.g, color.b));
                let offset = select(0.04, x - 6.25 * x * x, x < 0.08);
                var c = color - offset;
                let peak = max(c.r, max(c.g, c.b));
                if (peak < start_compression) {
                    return c;
                }
                let d = 1.0 - start_compression;
                let new_peak = 1.0 - d * d / (peak + d - start_compression);
                c *= new_peak / peak;
                let g = 1.0 - 1.0 / (desaturation * (peak - new_peak) + 1.0);
                return mix(c, vec3f(new_peak), g);
            }

            fn linear_to_srgb(color: vec3f) -> vec3f {
                let low = color * 12.92;
                let high = 1.055 * pow(color, vec3f(1.0 / 2.4)) - 0.055;
                return select(high, low, color <= vec3f(0.0031308));
            }

            @fragment
            fn fs_main(input: VertexOutput) -> @location(0) vec4f {
                let size = vec2f(textureDimensions(hdr_texture));
                let coords = vec2i(min(input.uv * size, size - 1.0));
                let hdr = textureLoad(hdr_texture, coords, 0).rgb * params.exposure;
                var color: vec3f;
                switch (u32(params.mode)) {
                    case 1u: { color = reinhard(hdr); }
                    case 2u: { color = aces(hdr); }
                    case 3u: { color = agx(hdr); }
                    case 4u: { color = neutral(hdr); }
                    default: { color = hdr; }
                }
                color = clamp(color, vec3f(0.0), vec3f(1.0));
                if (params.gamma_correct > 0.5) {
                    color = linear_to_srgb(color);
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
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, BufferBindingLayout()),
                ),
                label = "tonemapper bind group layout",
            )
        )

    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(bindGroupLayout), label = "tonemapper pipeline layout")
        )

    private val paramsData = FloatBuffer(4)
    private val paramsBuffer =
        device.createGPUFloatBuffer(
            "tonemapper params",
            paramsData,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    private val paramsBinding = BufferBinding(paramsBuffer)

    private var pipeline: RenderPipeline? = null
    private var pipelineDirty = true
    private var paramsDirty = true
    private var bindGroup: BindGroup? = null
    private var bindGroupSource: TextureView? = null

    /**
     * Tonemaps the [source] texture view and draws it as a fullscreen triangle into the current
     * [renderPassEncoder]. The render pass target must match [format]. The [source] must be a
     * filterable float texture, such as [TextureFormat.RGBA16_FLOAT].
     */
    fun render(renderPassEncoder: RenderPassEncoder, source: TextureView) {
        if (paramsDirty) {
            paramsData[0] = exposure
            paramsData[1] = mode.ordinal.toFloat()
            paramsData[2] = if (format.srgb || format.hdr) 0f else 1f
            paramsData[3] = 0f
            device.queue.writeBuffer(paramsBuffer, paramsData)
            paramsDirty = false
        }
        renderPassEncoder.setPipeline(getPipeline())
        renderPassEncoder.setBindGroup(0, getBindGroup(source))
        renderPassEncoder.draw(3, 1)
    }

    private fun getPipeline(): RenderPipeline {
        val current = pipeline
        if (current != null && !pipelineDirty) return current
        current?.release()
        pipelineDirty = false
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
                    label = "tonemapper pipeline",
                )
            )
            .also { pipeline = it }
    }

    private fun getBindGroup(source: TextureView): BindGroup {
        val current = bindGroup
        if (current != null && bindGroupSource === source) return current
        current?.release()
        bindGroupSource = source
        return device
            .createBindGroup(
                BindGroupDescriptor(
                    bindGroupLayout,
                    listOf(BindGroupEntry(0, source), BindGroupEntry(1, paramsBinding)),
                    label = "tonemapper bind group",
                )
            )
            .also { bindGroup = it }
    }

    override fun release() {
        bindGroup?.release()
        bindGroup = null
        bindGroupSource = null
        pipeline?.release()
        pipeline = null
        paramsBuffer.release()
        pipelineLayout.release()
        bindGroupLayout.release()
        shader.release()
    }
}

/**
 * The operator a [Tonemapper] uses to map HDR colors to a displayable range.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
enum class TonemapMode {
    /** No tonemapping. Colors are clamped to `[0, 1]`. */
    NONE,

    /** Simple Reinhard operator. Desaturates bright colors. */
    REINHARD,

    /** Fitted ACES filmic curve. High contrast & saturated. */
    ACES,

    /** AgX with the default look. Handles very bright & saturated colors gracefully. */
    AGX,

    /** Khronos PBR Neutral. Keeps base colors close to their authored values. */
    NEUTRAL,
}
