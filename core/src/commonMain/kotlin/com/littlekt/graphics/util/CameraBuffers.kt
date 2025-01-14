package com.littlekt.graphics.util

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.BindGroup
import com.littlekt.graphics.webgpu.BindGroupLayout
import com.littlekt.graphics.webgpu.BufferBinding

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
interface CameraBuffers : Releasable {
    /**
     * The size in which the underlying buffer should be multiplied by to handle dynamic camera
     * uniform values.
     */
    val cameraDynamicSize: Int
    val cameraUniformBufferBinding: BufferBinding

    val bindGroupLayout: BindGroupLayout

    val bindGroup: BindGroup

    override fun release() {
        bindGroup.release()
        bindGroupLayout.release()
    }
}
