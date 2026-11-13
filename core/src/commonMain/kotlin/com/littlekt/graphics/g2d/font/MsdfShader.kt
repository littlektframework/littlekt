package com.littlekt.graphics.g2d.font

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.align

/**
 * Looks of an [MsdfFont]. Widths are in ems, so they scale with the text. The weight, outline and
 * glow together reach at most half of the font's distance range divided by its field size, and are
 * clamped to fit. Use a larger distance range for wider effects.
 *
 * @property weight grows (positive) or thins (negative) the glyphs
 * @property outlineWidth the width of the outline around the glyphs, or `0` for none
 * @property outlineColor the color of the outline
 * @property glowWidth the width of a soft glow around the glyphs and outline, or `0` for none
 * @property glowColor the color of the glow
 * @author Colton Daily
 * @date 11/12/2026
 */
data class MsdfStyle(
    val weight: Float = 0f,
    val outlineWidth: Float = 0f,
    val outlineColor: Color = Color.BLACK,
    val glowWidth: Float = 0f,
    val glowColor: Color = Color(0f, 0f, 0f, 0.6f),
)

/**
 * The batch [Shader] that draws an [MsdfFont] with a [MsdfStyle].
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
class MsdfShader
internal constructor(device: Device, style: MsdfStyle, distanceRange: Float, emToField: Float) :
    Shader(
        device,
        src =
            """
        struct CameraUniform {
            view_proj: mat4x4<f32>
        };
        @group(0) @binding(0)
        var<uniform> camera: CameraUniform;

        struct VertexOutput {
            @location(0) color: vec4<f32>,
            @location(1) uv: vec2<f32>,
            @builtin(position) position: vec4<f32>,
        };

        @vertex
        fn vs_main(
            @location(0) pos: vec3<f32>,
            @location(1) color: vec4<f32>,
            @location(2) uvs: vec2<f32>) -> VertexOutput {
            var output: VertexOutput;
            output.position = camera.view_proj * vec4<f32>(pos.x, pos.y, pos.z, 1);
            output.color = color;
            output.uv = uvs;
            return output;
        }

        @group(1) @binding(0)
        var my_texture: texture_2d<f32>;
        @group(1) @binding(1)
        var my_sampler: sampler;

        struct Style {
            params: vec4<f32>,
            outline_color: vec4<f32>,
            glow_color: vec4<f32>,
        };
        @group(2) @binding(0)
        var<uniform> style: Style;

        fn median(r: f32, g: f32, b: f32) -> f32 {
            return max(min(r, g), min(max(r, g), b));
        }

        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
            let s = textureSample(my_texture, my_sampler, in.uv);
            let unit_range = vec2<f32>(style.params.x) / vec2<f32>(textureDimensions(my_texture));
            let screen_size = vec2<f32>(1.0) / fwidth(in.uv);
            let px_range = max(0.5 * dot(unit_range, screen_size), 1.0);
            let weight = style.params.y;
            let fill = clamp((median(s.r, s.g, s.b) - 0.5 + weight) * px_range + 0.5, 0.0, 1.0);
            var color = in.color.rgb;
            var alpha = fill * in.color.a;
            let outline_width = style.params.z;
            if (outline_width > 0.0) {
                let outline = clamp((s.a - 0.5 + weight + outline_width) * px_range + 0.5, 0.0, 1.0)
                    * style.outline_color.a * in.color.a;
                color = mix(style.outline_color.rgb, in.color.rgb, fill);
                alpha = alpha + outline * (1.0 - alpha);
            }
            let glow_width = style.params.w;
            if (glow_width > 0.0) {
                let glow_distance = s.a - 0.5 + weight + outline_width;
                let glow = smoothstep(-glow_width, 0.0, glow_distance) * style.glow_color.a * in.color.a;
                let combined = alpha + glow * (1.0 - alpha);
                color = (color * alpha + style.glow_color.rgb * glow * (1.0 - alpha)) / max(combined, 0.0001);
                alpha = combined;
            }
            return vec4<f32>(color, alpha);
        }
        """
                .trimIndent(),
        bindGroupLayoutUsageLayout = listOf(BindingUsage.CAMERA, BindingUsage.TEXTURE, STYLE),
        layout =
            mapOf(
                BindingUsage.CAMERA to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(
                                0,
                                ShaderStage.VERTEX,
                                BufferBindingLayout(
                                    hasDynamicOffset = true,
                                    minBindingSize =
                                        (Float.SIZE_BYTES * 16)
                                            .align(device.limits.minUniformBufferOffsetAlignment)
                                            .toLong(),
                                ),
                            )
                        ),
                        label = "MsdfShader camera bind group layout",
                    ),
                BindingUsage.TEXTURE to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        ),
                        label = "MsdfShader texture bind group layout",
                    ),
                STYLE to
                    BindGroupLayoutDescriptor(
                        listOf(BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout())),
                        label = "MsdfShader style bind group layout",
                    ),
            ),
    ) {

    private val styleData = FloatBuffer(12)
    private val styleBuffer =
        device.createGPUFloatBuffer(
            "MsdfShader style",
            styleData.also { writeStyle(it, style, distanceRange, emToField) },
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    private val styleBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                getBindGroupLayoutByUsage(STYLE),
                listOf(BindGroupEntry(0, BufferBinding(styleBuffer))),
                label = "MsdfShader style bind group",
            )
        )

    override fun setBindGroups(renderPassEncoder: RenderPassEncoder) {
        setBindGroup(renderPassEncoder, styleBindGroup, STYLE)
    }

    override fun release() {
        styleBindGroup.release()
        styleBuffer.release()
        super.release()
    }

    private companion object {
        val STYLE = BindingUsage("MSDF Style")
        const val MAX_REACH = 0.45f

        fun writeStyle(data: FloatBuffer, style: MsdfStyle, range: Float, emToField: Float) {
            val max = MAX_REACH
            val weight = (style.weight * emToField).coerceIn(-max, max)
            val outline = (style.outlineWidth * emToField).coerceIn(0f, max - weight)
            val glow = (style.glowWidth * emToField).coerceIn(0f, max - weight - outline)
            data[0] = range
            data[1] = weight
            data[2] = outline
            data[3] = glow
            data[4] = style.outlineColor.r
            data[5] = style.outlineColor.g
            data[6] = style.outlineColor.b
            data[7] = style.outlineColor.a
            data[8] = style.glowColor.r
            data[9] = style.glowColor.g
            data[10] = style.glowColor.b
            data[11] = style.glowColor.a
        }
    }
}
