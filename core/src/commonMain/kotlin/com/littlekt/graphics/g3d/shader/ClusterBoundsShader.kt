package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.g3d.util.CameraBuffers
import com.littlekt.graphics.g3d.util.shader.buildCommonShader
import com.littlekt.graphics.g3d.util.shader.camera
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @param cameraBuffers a [CameraBuffers] instance that is generally shared with a
 *   [ClusterLightsShader]. Calling [release] on this shader will NOT release the instance and
 *   should be called when ready to dispose of it.
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusterBoundsShader(
    device: Device,
    val cameraBuffers: CameraBuffers,
    computeEntryPoint: String = "cmp_main",
    computeSrc: String = buildCommonShader {
        compute {
            clusteredBounds {
                camera(0, 0)
                cluster(1, 0, MemoryAccessMode.READ_WRITE)
                main(computeEntryPoint)
            }
        }
    },
    bindGroupLayout: List<BindGroupLayoutDescriptor> =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.COMPUTE, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // cluster bounds
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.COMPUTE,
                        BufferBindingLayout(type = BufferBindingType.STORAGE),
                    )
                )
            ),
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
                    listOf(BindGroupEntry(0, cameraBuffers.cameraUniformBufferBinding)),
                )
            )
        )

        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[0],
                    listOf(
                        BindGroupEntry(
                            0,
                            cameraBuffers.clusterBuffers.clusterBoundsStorageBufferBinding,
                        )
                    ),
                )
            )
        )
    }

    override fun release() {
        super.release()
    }
}
