package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.g3d.util.CameraBuffers
import com.littlekt.graphics.g3d.util.ClusterBuffers
import com.littlekt.graphics.g3d.util.LightBuffer
import com.littlekt.graphics.g3d.util.shader.buildCommonShader
import com.littlekt.graphics.g3d.util.shader.camera
import com.littlekt.graphics.g3d.util.shader.light
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @param cameraBuffers a [CameraBuffers] instance that is generally shared with a
 *   [ClusterBoundsShader]. Calling [release] on this shader will NOT release the instance and
 *   should be called when ready to dispose of it.
 * @param clusterBuffers a [ClusterBuffers] instance that is generally shared with a
 *   [ClusterBoundsShader]. Calling [release] on this shader will NOT release the instance and
 *   should be called when ready to dispose of it.
 * @param lightBuffer a [LightBuffer] instance that is generally shared. Calling [release] on this
 *   shader will NOT release the instance and should be called when ready to dispose of it.
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusterLightsShader(
    device: Device,
    val cameraBuffers: CameraBuffers,
    val clusterBuffers: ClusterBuffers,
    val lightBuffer: LightBuffer,
    computeEntryPoint: String = "cmp_main",
    computeSrc: String = buildCommonShader {
        compute {
            clusteredLight {
                camera(0, 0)
                cluster(0, 1, MemoryAccessMode.READ)
                clusterLights(0, 2, MemoryAccessMode.READ_WRITE)
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
    ) {

    override fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>) {
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[0],
                    listOf(
                        BindGroupEntry(0, cameraBuffers.cameraUniformBufferBinding),
                        BindGroupEntry(1, clusterBuffers.clusterBoundsStorageBufferBinding),
                        BindGroupEntry(2, clusterBuffers.clusterLightsStorageBufferBinding),
                        BindGroupEntry(3, lightBuffer.bufferBinding),
                    ),
                )
            )
        )
    }

    override fun release() {
        super.release()
    }
}
