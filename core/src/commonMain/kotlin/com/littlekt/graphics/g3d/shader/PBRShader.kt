package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.buildCommonShader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
open class PBRShader(
    device: Device,
    layout: List<VertexAttribute>,
    baseColorTexture: Texture,
    baseColorFactor: Color = Color.WHITE,
    transparent: Boolean = false,
    doubleSided: Boolean = false,
    alphaCutoff: Float = 0f,
    castShadows: Boolean = true,
    depthWrite: Boolean = true,
    depthCompareFunction: CompareFunction = CompareFunction.LESS,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String = buildCommonShader {
        vertex {
            vertexInput(layout)
            vertexOutput(layout)
            camera(0, 0)
            model(1, 0)
            main(layout, vertexEntryPoint)
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
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
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
    UnlitShader(
        device,
        layout,
        baseColorTexture,
        baseColorFactor,
        transparent,
        doubleSided,
        alphaCutoff,
        castShadows,
        depthWrite,
        depthCompareFunction,
        vertexEntryPoint,
        fragmentEntryPoint,
        vertexSrc,
        fragmentSrc,
        bindGroupLayout,
    ) {}
