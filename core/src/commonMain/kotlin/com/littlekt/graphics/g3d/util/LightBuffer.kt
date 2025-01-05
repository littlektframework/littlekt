package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.align

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class LightBuffer(val device: Device, maxLightCount: Int) : Releasable {
    private val lightBufferByteSize =
        AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE + (POINT_LIGHT_SIZE * maxLightCount)
    private val lightsBuffer = FloatBuffer(lightBufferByteSize)

    /** The [GPUBuffer] that holds the light data */
    val buffer = run {
        val buffer =
            device.createBuffer(
                BufferDescriptor(
                    "light",
                    (Float.SIZE_BYTES * lightBufferByteSize)
                        .align(device.limits.minUniformBufferOffsetAlignment)
                        .toLong() * 4,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                    true,
                )
            )
        buffer.getMappedRange().putFloat(lightsBuffer)
        buffer.unmap()

        buffer
    }

    /** The [BufferBinding] for [buffer]. */
    val bufferBinding =
        BufferBinding(buffer, size = lightBufferByteSize * Float.SIZE_BYTES.toLong())

    companion object {
        const val AMBIENT_LIGHT_SIZE = 4
        const val DIRECTIONAL_LIGHT_SIZE = 8
        const val POINT_LIGHT_SIZE = 8
    }

    override fun release() {
        buffer.release()
    }
}
