package com.littlekt.graphics.g2d

import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.align

/**
 * The default [Shader] that is used in [SpriteBatch].
 *
 * @param device the current [Device]
 * @author Colton Daily
 * @date 4/15/2024
 */
class SpriteBatchShader(device: Device) :
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

        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
            return textureSample(my_texture, my_sampler, in.uv) * in.color;
        }
        """
                .trimIndent(),
        bindGroupLayoutUsageLayout = listOf(BindingUsage.CAMERA, BindingUsage.TEXTURE),
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
                        label = "SpriteBatch Camera BindGroupLayoutDescriptor",
                    ),
                BindingUsage.TEXTURE to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        ),
                        label = "SpriteBatchShader texture BindGroupLayoutDescriptor",
                    ),
            ),
    )
