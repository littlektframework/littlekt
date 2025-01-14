package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
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
    bindGroupLayoutUsageLayout: List<BindingUsage> = listOf(BindingUsage.CLUSTER_LIGHTS),
    bindGroupLayout: Map<BindingUsage, BindGroupLayoutDescriptor> = emptyMap(),
) :
    Shader(
        device = device,
        src = computeSrc,
        bindGroupLayoutUsageLayout = bindGroupLayoutUsageLayout,
        layout = bindGroupLayout,
        computeEntryPoint = computeEntryPoint,
    )
