package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.g3d.util.shader.buildCommonShader
import com.littlekt.graphics.g3d.util.shader.cameraWithLights
import com.littlekt.graphics.g3d.util.shader.cluster
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusterBoundsShader(
    device: Device,
    computeEntryPoint: String = "cmp_main",
    computeSrc: String = buildCommonShader {
        compute {
            clusteredBounds {
                cameraWithLights(0, 0)
                cluster(1, 0, access = MemoryAccessMode.READ_WRITE)
                main(computeEntryPoint)
            }
        }
    },
    bindGroupLayoutUsageLayout: List<BindingUsage> =
        listOf(BindingUsage.CAMERA, BindingUsage.CLUSTER_BOUNDS),
    bindGroupLayout: Map<BindingUsage, BindGroupLayoutDescriptor> =
        mapOf(
            BindingUsage.CLUSTER_BOUNDS to
                BindGroupLayoutDescriptor(
                    listOf(
                        // cluster bounds
                        BindGroupLayoutEntry(
                            0,
                            ShaderStage.COMPUTE,
                            BufferBindingLayout(type = BufferBindingType.STORAGE),
                        )
                    ),
                    label = "Cluster Bounds Storage BindGroupLayout",
                )
        ),
) :
    Shader(
        device = device,
        src = computeSrc,
        bindGroupLayoutUsageLayout = bindGroupLayoutUsageLayout,
        layout = bindGroupLayout,
        computeEntryPoint = computeEntryPoint,
    )
