package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
class PBRShader(
    device: Device,
    layout: List<VertexAttribute>,
    skinned: Boolean,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String = buildCommonShader {
        vertex {
            vertexInput(layout)
            vertexOutput(layout)
            cameraComplex(0, 0)
            models(1, 0)
            main(
                layout,
                cameraViewProjCombined = false,
                skinned = skinned,
                skinGroup = 3,
                entryPoint = vertexEntryPoint,
            )
        }
    },
    fragmentSrc: String = buildCommonShader {
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
    },
    bindGroupLayoutUsageLayout: List<BindingUsage> = run {
        val usages = mutableListOf(BindingUsage.CAMERA, BindingUsage.MODEL, BindingUsage.MATERIAL)
        if (skinned) {
            usages += BindingUsage.SKIN
        }
        usages
    },
    bindGroupLayout: Map<BindingUsage, BindGroupLayoutDescriptor> = run {
        val layouts =
            mutableMapOf(
                BindingUsage.MODEL to
                    BindGroupLayoutDescriptor(
                        listOf(
                            // model
                            BindGroupLayoutEntry(
                                0,
                                ShaderStage.VERTEX,
                                BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                            )
                        )
                    ),
                BindingUsage.MATERIAL to
                    BindGroupLayoutDescriptor(
                        listOf(
                            // material uniform
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                            // baseColorTexture
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
            )
        if (skinned) {
            layouts[BindingUsage.SKIN] =
                BindGroupLayoutDescriptor(
                    listOf(
                        // joint transforms
                        BindGroupLayoutEntry(
                            0,
                            ShaderStage.VERTEX,
                            BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                        ),
                        // inverse blend matrices
                        BindGroupLayoutEntry(
                            1,
                            ShaderStage.VERTEX,
                            BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                        ),
                    )
                )
        }
        layouts
    },
) :
    Shader(
        device = device,
        src = "$vertexSrc\n$fragmentSrc",
        bindGroupLayoutUsageLayout = bindGroupLayoutUsageLayout,
        layout = bindGroupLayout,
        vertexEntryPoint = vertexEntryPoint,
        fragmentEntryPoint = fragmentEntryPoint,
    )
