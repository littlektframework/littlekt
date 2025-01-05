package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.BufferBinding

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
interface CameraBuffers : Releasable {
    val cameraUniformBufferBinding: BufferBinding

    fun updateCameraUniform(camera: Camera)
}
