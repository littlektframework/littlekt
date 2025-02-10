package com.littlekt.graphics.g2d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffersViaMatrix
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4
import com.littlekt.util.align
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/13/2025
 */
class CameraSpriteBuffers(val device: Device, override val cameraDynamicSize: Int = 50) :
    CameraBuffersViaMatrix {
    private val camFloatBuffer = FloatBuffer(16)

    init {
        check(cameraDynamicSize >= 1) { "CameraSpriteBuffers: 'cameraDynamicSize' must be >= 1!" }
    }

    /** The [GPUBuffer] that holds the matrix data. */
    val cameraUniformBuffer = run {
        val buffer =
            device.createBuffer(
                BufferDescriptor(
                    "CameraSpriteBuffers viewProj Buffer",
                    (Float.SIZE_BYTES * 16)
                        .align(device.limits.minUniformBufferOffsetAlignment)
                        .toLong() * cameraDynamicSize,
                    BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                    true,
                )
            )
        buffer.getMappedRange().putFloat(camFloatBuffer.toArray())
        buffer.unmap()

        buffer
    }

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    override val cameraUniformBufferBinding =
        BufferBinding(
            cameraUniformBuffer,
            size =
                (Float.SIZE_BYTES * 16)
                    .align(device.limits.minUniformBufferOffsetAlignment)
                    .toLong(),
        )
    private val bindGroupLayout: BindGroupLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(
                            hasDynamicOffset = true,
                            minBindingSize =
                                (Float.SIZE_BYTES * 16)
                                    .align(device.limits.minUniformBufferOffsetAlignment)
                                    .toLong(),
                        ),
                    )
                ),
                label = "CameraSpriteBuffers viewProj BindGroupLayout",
            )
        )
    private val bindGroup: BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                bindGroupLayout,
                listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
                label = "CameraSpriteBuffers viewProj BindGroup",
            )
        )
    override val bindingUsage: BindingUsage = BindingUsage.CAMERA

    override fun getOrCreateBindGroup(shader: Shader): BindGroup {
        return bindGroup
    }

    override fun update(viewProj: Mat4, dt: Duration, dynamicOffset: Long) {
        device.queue.writeBuffer(
            cameraUniformBuffer,
            viewProj.toBuffer(camFloatBuffer),
            offset = dynamicOffset * device.limits.minUniformBufferOffsetAlignment,
        )
    }

    override fun release() {
        bindGroupLayout.release()
        bindGroup.release()
        cameraUniformBuffer.release()
    }
}
