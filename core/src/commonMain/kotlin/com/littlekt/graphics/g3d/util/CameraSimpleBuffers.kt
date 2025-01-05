package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.BufferBinding
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class CameraSimpleBuffers(val device: Device) : CameraBuffers {
    private val camFloatBuffer = FloatBuffer(BUFFER_SIZE)

    /**
     * The [GPUBuffer] that holds the camera view-projection matrix data.
     *
     * @see updateCameraUniform
     */
    val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera",
            camFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    override val cameraUniformBufferBinding =
        BufferBinding(cameraUniformBuffer, size = Float.SIZE_BYTES * BUFFER_SIZE.toLong())

    override fun updateCameraUniform(camera: Camera) =
        device.queue.writeBuffer(
            cameraUniformBuffer,
            camera.viewProjection.toBuffer(camFloatBuffer),
        )

    override fun release() {
        cameraUniformBuffer.release()
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val BUFFER_SIZE = 16
    }
}
