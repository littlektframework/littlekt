package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 2/7/2025
 */
class StandardVertexShader(device: Device, layout: List<VertexAttribute>, skinned: Boolean) :
    Shader(
        device = device,
        src = Standard.VertexShader(layout).src,
        bindGroupLayoutUsageLayout =
            run {
                val usages = mutableListOf(BindingUsage.CAMERA, BindingUsage.MODEL)
                if (skinned) {
                    usages += BindingUsage.SKIN
                }
                usages
            },
        layout =
            run {
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
    )
