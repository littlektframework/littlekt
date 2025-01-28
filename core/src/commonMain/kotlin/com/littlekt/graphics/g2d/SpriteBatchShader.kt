package com.littlekt.graphics.g2d

import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.SpriteShader
import com.littlekt.util.align
import io.ygdrasil.webgpu.BindGroup
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupDescriptor.*
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry.*
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.RenderPassEncoder
import io.ygdrasil.webgpu.ShaderStage

/**
 * The default [SpriteShader] that is used [SpriteBatch].
 *
 * @param device the current [Device]
 * @param cameraDynamicSize the size in which the underlying [cameraUniformBuffer] should be
 *   multiplied by to handle dynamic camera uniform values.
 * @author Colton Daily
 * @date 4/15/2024
 */
class SpriteBatchShader(device: Device, cameraDynamicSize: Int = 50) :
    SpriteShader(
        device,
        // language=wgsl
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
        layout =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    Entry(
                        0u,
                        setOf(ShaderStage.Vertex),
                        BufferBindingLayout(
                            hasDynamicOffset = true,
                            minBindingSize =
                            (Float.SIZE_BYTES * 16)
                                .align(device.limits.minUniformBufferOffsetAlignment.toInt())
                                .toULong()
                        )
                    )
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    Entry(
                        0u, setOf(ShaderStage.Fragment),
                        TextureBindingLayout()
                    ),
                    Entry(
                        1u, setOf(ShaderStage.Fragment),
                        SamplerBindingLayout()
                    )
                )
            )
        ),
        cameraDynamicSize = cameraDynamicSize
    ) {

    override fun MutableList<BindGroup>.createBindGroupsWithTexture(
        texture: Texture,
        data: Map<String, Any>
    ) {
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[0],
                    listOf(BindGroupEntry(0u, cameraUniformBufferBinding))
                )
            )
        )
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[1],
                    listOf(
                        BindGroupEntry(0u, TextureViewBinding(texture.view)),
                        BindGroupEntry(1u, SamplerBinding(texture.sampler))
                    )
                )
            )
        )
    }

    override fun setBindGroups(
        encoder: RenderPassEncoder,
        bindGroups: List<BindGroup>,
        dynamicOffsets: List<Long>
    ) {
        encoder.setBindGroup(0u, bindGroups[0], dynamicOffsets.map { it.toUInt() })
        encoder.setBindGroup(1u, bindGroups[1])
    }
}
