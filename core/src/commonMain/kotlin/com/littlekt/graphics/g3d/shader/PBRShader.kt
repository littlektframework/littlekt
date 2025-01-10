package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
class PBRShader(
    device: Device,
    layout: List<VertexAttribute>,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String =
        buildCommonShader {
                vertex {
                    vertexInput(layout)
                    vertexOutput(layout)
                    cameraWithLights(0, 0)
                    model(1, 0)
                    skin(2)
                    main(layout, cameraViewProjCombined = false, entryPoint = vertexEntryPoint)
                }
            }
            .also { println(it) },
    fragmentSrc: String =
        buildCommonShader {
                fragment {
                    pbr {
                        light(0, 1)
                        clusterLights(0, 2)
                        material(2)
                        tileFunctions()
                        surfaceInfo(layout)
                        main()
                    }
                }
            }
            .also { println(it) },
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
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // material uniform
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                    // baseColorTexturere
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // baseColorSampler
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // normal texture
                    BindGroupLayoutEntry(3, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // normal sampler
                    BindGroupLayoutEntry(4, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // metallic roughness texture
                    BindGroupLayoutEntry(5, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // metallic roughness sampler
                    BindGroupLayoutEntry(6, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // occlusion texture
                    BindGroupLayoutEntry(7, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // occlusion sampler
                    BindGroupLayoutEntry(8, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // emissive texture
                    BindGroupLayoutEntry(9, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // emissive sampler
                    BindGroupLayoutEntry(10, ShaderStage.FRAGMENT, SamplerBindingLayout()),
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
