package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.seconds
import kotlin.time.Duration

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
     * The [GPUBuffer] that holds the camera data.
     *
     * @see update
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
                            BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout()),
                            // light
                            BindGroupLayoutEntry(
                                1,
                                ShaderStage.FRAGMENT,
                                BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                            ),
                            // cluster lights
                            BindGroupLayoutEntry(
                                2,
                                ShaderStage.FRAGMENT or ShaderStage.COMPUTE,
                                BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                            ),
                        )
                    )
                ),
                listOf(
                    BindGroupEntry(0, cameraUniformBufferBinding),
                    BindGroupEntry(1, lightBuffer.bufferBinding),
                    BindGroupEntry(2, clusterBuffers.clusterLightsStorageBufferBinding),
                ),
            )
        )

    override fun update(camera: Camera, dt: Duration) {
        camFloatBuffer.put(camera.projection.data, 0)
        camFloatBuffer.put(camera.invProj.data, 16 * Float.SIZE_BYTES)
        camFloatBuffer.put(camera.view.data, 32 * Float.SIZE_BYTES)
        camFloatBuffer.put(camera.position.fields, 48 * Float.SIZE_BYTES)
        camFloatBuffer[51 * Float.SIZE_BYTES] = dt.seconds
        camFloatBuffer[52 * Float.SIZE_BYTES] = camera.virtualWidth
        camFloatBuffer[53 * Float.SIZE_BYTES] = camera.virtualHeight
        camFloatBuffer[54 * Float.SIZE_BYTES] = camera.near
        camFloatBuffer[55 * Float.SIZE_BYTES] = camera.far
        device.queue.writeBuffer(cameraUniformBuffer, camFloatBuffer)
    }

    override fun release() {
        cameraUniformBuffer.release()
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val BUFFER_SIZE = 56
    }
}
