package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.BindGroup
import com.littlekt.graphics.webgpu.BindGroupLayout
import com.littlekt.graphics.webgpu.BufferBinding
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
interface CameraBuffers : Releasable {
    val cameraUniformBufferBinding: BufferBinding

    val bindGroupLayout: BindGroupLayout

    val bindGroup: BindGroup

    fun update(camera: Camera, dt: Duration)
}
