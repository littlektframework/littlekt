package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.BufferBinding
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer
import com.littlekt.math.Vec3f

/**
 * A class for managing GPU light data in a buffer, supporting different types of lights such as
 * ambient, directional, and point lights. The data is stored as a structured collection of floats
 * and can be updated and sent to the GPU.
 *
 * @property device The device used for creating GPU buffers and managing the queue.
 * @property maxLightCount The maximum number of point lights this buffer can store.
 */
class LightBuffer(val device: Device, val maxLightCount: Int) : Releasable {
    /** Size of light buffer in number of components / floats. */
    private val lightBufferSize =
        AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE + (POINT_LIGHT_SIZE * maxLightCount)
    private val lightsBuffer = ByteBuffer(lightBufferSize * Float.SIZE_BYTES)

    /** The [GPUBuffer] that holds the light data */
    val buffer =
        device.createGPUByteBuffer(
            "light",
            lightsBuffer,
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [buffer]. */
    val bufferBinding = BufferBinding(buffer)

    fun update() {
        device.queue.writeBuffer(buffer, lightsBuffer)
    }

    fun ambient(color: Color) {
        lightsBuffer.position = 0
        lightsBuffer.putFloat(color.r)
        lightsBuffer.putFloat(color.g)
        lightsBuffer.putFloat(color.b)
        lightsBuffer.putFloat(0f) // padding
    }

    fun dirColor(color: Color) {
        lightsBuffer.position = 4 * Float.SIZE_BYTES
        lightsBuffer.putFloat(color.r)
        lightsBuffer.putFloat(color.g)
        lightsBuffer.putFloat(color.b)
    }

    fun dirIntensity(intensity: Float) {
        lightsBuffer.position = 7 * Float.SIZE_BYTES
        lightsBuffer.putFloat(intensity)
    }

    fun dirDirection(direction: Vec3f) {
        lightsBuffer.position = 8 * Float.SIZE_BYTES
        lightsBuffer.putFloat(direction.x)
        lightsBuffer.putFloat(direction.y)
        lightsBuffer.putFloat(direction.z)
    }

    fun pointLight(index: Int, position: Vec3f, color: Color, intensity: Float, range: Float) {
        if (index >= maxLightCount) {
            return
        }
        if (intensity > 0) {
            val offset =
                POINT_LIGHT_OFFSET * Float.SIZE_BYTES +
                    (index * POINT_LIGHT_SIZE * Float.SIZE_BYTES)
            lightsBuffer.position = offset
            lightsBuffer.putFloat(position.x)
            lightsBuffer.putFloat(position.y)
            lightsBuffer.putFloat(position.z)
            lightsBuffer.putFloat(range)
            lightsBuffer.putFloat(color.r)
            lightsBuffer.putFloat(color.g)
            lightsBuffer.putFloat(color.b)
            lightsBuffer.putFloat(intensity)
            incrementLightCount()
        }
    }

    private fun incrementLightCount() {
        lightsBuffer.position = 11 * Float.SIZE_BYTES
        val currentValue = lightsBuffer.readInt
        lightsBuffer.position = 11 * Float.SIZE_BYTES
        lightsBuffer.putInt(currentValue + 1)
    }

    fun resetLightCount() {
        lightsBuffer.position = 11 * Float.SIZE_BYTES
        lightsBuffer.putInt(0)
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val AMBIENT_LIGHT_SIZE = 4

        /** Size in total floats (NOT BYTE SIZE). */
        const val DIRECTIONAL_LIGHT_SIZE = 8

        /** Size in total floats (NOT BYTE SIZE). */
        const val POINT_LIGHT_SIZE = 8

        private const val POINT_LIGHT_OFFSET = AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE
    }

    override fun release() {
        buffer.release()
    }
}
