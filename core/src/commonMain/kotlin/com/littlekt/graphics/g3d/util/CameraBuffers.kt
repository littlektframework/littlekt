package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.BufferBinding
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer
import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class CameraBuffers(
    val device: Device,
    val lightBuffer: LightBuffer,
    val clusterBuffers: ClusterBuffers = ClusterBuffers(device),
) : Releasable {
    private val camFloatBuffer = FloatBuffer(16)

    /**
     * The [GPUBuffer] that holds the camera view-projection matrix data.
     *
     * @see updateCameraUniform
     */
    val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera.viewProj",
            camFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    val cameraUniformBufferBinding =
        BufferBinding(cameraUniformBuffer, size = Float.SIZE_BYTES * 16L)

    fun updateCameraUniform(viewProjection: Mat4) =
        device.queue.writeBuffer(cameraUniformBuffer, viewProjection.toBuffer(camFloatBuffer))

    override fun release() {
        cameraUniformBuffer.release()
    }
}
