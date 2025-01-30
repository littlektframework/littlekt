package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.graphics.webgpu.*
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class CameraSimpleBuffers(val device: Device) : CameraBuffersViaCamera {
    private val camFloatBuffer = FloatBuffer(BUFFER_SIZE)

    /**
     * The [GPUBuffer] that holds the camera view-projection matrix data.
     *
     * @see updateCameraUniform
     */
    val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera",
            camFloatBuffer,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    override val cameraDynamicSize: Int = 1

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    override val cameraUniformBufferBinding =
        BufferBinding(cameraUniformBuffer, size = Float.SIZE_BYTES * BUFFER_SIZE.toLong())

    override val bindGroupLayout: BindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            )
        )

    /** The camera uniform bind group. */
    override val bindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                bindGroupLayout,
                listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
            )
        )

    override fun update(camera: Camera, dt: Duration, dynamicOffset: Long) =
        device.queue.writeBuffer(
            cameraUniformBuffer,
            camera.viewProjection.toBuffer(camFloatBuffer),
        )

    override fun release() {
        super.release()
        cameraUniformBuffer.release()
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val BUFFER_SIZE = 16
    }
}
