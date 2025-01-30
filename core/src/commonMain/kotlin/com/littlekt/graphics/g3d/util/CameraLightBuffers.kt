package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.util.CameraBuffersViaCamera
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
) : CameraBuffersViaCamera {
    private val camFloatBuffer = FloatBuffer(BUFFER_SIZE) // 56 floats required

    /**
     * The [GPUBuffer] that holds the camera data.
     *
     * @see update
     */
    val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera",
            camFloatBuffer,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    override val cameraDynamicSize: Int = 1

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    override val cameraUniformBufferBinding = BufferBinding(cameraUniformBuffer)

    override val bindGroupLayout: BindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT or ShaderStage.COMPUTE,
                        BufferBindingLayout(),
                    ),
                    // light
                    BindGroupLayoutEntry(
                        1,
                        ShaderStage.VERTEX or ShaderStage.FRAGMENT or ShaderStage.COMPUTE,
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
        )

    /** The camera uniform bind group. */
    override val bindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                bindGroupLayout,
                listOf(
                    BindGroupEntry(0, cameraUniformBufferBinding),
                    BindGroupEntry(1, lightBuffer.bufferBinding),
                    BindGroupEntry(2, clusterBuffers.clusterLightsStorageBufferBinding),
                ),
                label = "CameraLightBuffers Bind Group",
            )
        )

    override fun update(camera: Camera, dt: Duration, dynamicOffset: Long) {
        camFloatBuffer.put(camera.projection.data, dstOffset = 0)
        camFloatBuffer.put(camera.invProj.data)
        camFloatBuffer.put(camera.view.data)
        camFloatBuffer.put(camera.position.fields)
        camFloatBuffer.put(dt.seconds)
        camFloatBuffer.put(camera.virtualWidth)
        camFloatBuffer.put(camera.virtualHeight)
        camFloatBuffer.put(camera.near)
        camFloatBuffer.put(camera.far)
        device.queue.writeBuffer(cameraUniformBuffer, camFloatBuffer)
    }

    override fun release() {
        super.release()
        cameraUniformBuffer.release()
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val BUFFER_SIZE = 56
    }
}
