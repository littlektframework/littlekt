package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 2/5/2025
 */
class DepthSliceShader(
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
                skinGroup = 2,
                entryPoint = vertexEntryPoint,
            )
        }
    },
    fragmentSrc: String = buildCommonShader {
        fragment { from(DepthSliceMaterialBuilder()) { main() } }
    },
    bindGroupLayoutUsageLayout: List<BindingUsage> = run {
        val usages = mutableListOf(BindingUsage.CAMERA, BindingUsage.MODEL)
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
                    )
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
