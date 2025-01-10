package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @param cameraBuffers a [CameraLightBuffers] instance that is generally shared with a
 *   [ClusterBoundsShader]. Calling [release] on this shader will NOT release the instance and
 *   should be called when ready to dispose of it.
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusterLightsShader(
    device: Device,
    computeEntryPoint: String = "cmp_main",
    computeSrc: String = buildCommonShader {
        compute {
            clusteredLight {
                cameraWithLights(0, 0)
                cluster(0, 1, access = MemoryAccessMode.READ)
                clusterLights(0, 2, access = MemoryAccessMode.READ_WRITE)
                light(0, 3)
                tileFunctions()
                main(computeEntryPoint)
            }
        }
    },
    bindGroupLayout: List<BindGroupLayoutDescriptor> =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.COMPUTE, BufferBindingLayout()),
                    // cluster bounds
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                    ),
                    // cluster lights
                    BindGroupLayoutEntry(
                        2,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(type = BufferBindingType.STORAGE),
                    ),
                    // lights
                    BindGroupLayoutEntry(
                        3,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                    ),
                )
            )
        ),
) :
    Shader(
        device = device,
        src = computeSrc,
        layout = bindGroupLayout,
        computeEntryPoint = computeEntryPoint,
    )
