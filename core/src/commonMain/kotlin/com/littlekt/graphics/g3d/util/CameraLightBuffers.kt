package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class CameraLightBuffers(
    val device: Device,
    val lightBuffer: LightBuffer,
    val clusterBuffers: ClusterBuffers = ClusterBuffers(device),
) : CameraBuffers {
    private val camFloatBuffer = FloatBuffer(BUFFER_SIZE) // 56 floats required

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

    /** The camera uniform bind group. */
    override val bindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        listOf(
                            // camera
                            BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                        )
                    )
                ),
                listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
            )
        )

    // todo update to use all required camera fields (near, far, etc)
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
        const val BUFFER_SIZE = 56
    }
}
