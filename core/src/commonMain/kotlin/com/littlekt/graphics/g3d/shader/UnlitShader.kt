package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/10/2024
 */
class UnlitShader(
    device: Device,
    layout: List<VertexAttribute>,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String = buildCommonShader {
        vertex {
            vertexInput(layout)
            vertexOutput(layout)
            camera(0, 0)
            model(1, 0)
            main(layout, entryPoint = vertexEntryPoint)
        }
    },
    fragmentSrc: String = buildCommonShader {
        fragment {
            unlit {
                material(2)
                main()
            }
        }
    },
    bindGroupLayout: List<BindGroupLayoutDescriptor> =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // model
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.UNIFORM),
                    )
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // material uniform
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                    // baseColorTexture
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // baseColorSampler
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                )
            ),
        ),
) :
    Shader(
        device = device,
        src = "$vertexSrc\n$fragmentSrc",
        layout = bindGroupLayout,
        vertexEntryPoint = vertexEntryPoint,
        fragmentEntryPoint = fragmentEntryPoint,
    )
