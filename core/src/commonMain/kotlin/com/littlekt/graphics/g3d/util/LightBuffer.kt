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
 * @author Colton Daily
 * @date 1/5/2025
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
            lightsBuffer.toArray(),
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
        //        lightsBuffer[0] = color.r
        //        lightsBuffer[1] = color.g
        //        lightsBuffer[2] = color.b
        //        lightsBuffer[3] = 0f // padding
    }

    fun dirColor(color: Color) {
        lightsBuffer.position = 4 * Float.SIZE_BYTES
        lightsBuffer.putFloat(color.r)
        lightsBuffer.putFloat(color.g)
        lightsBuffer.putFloat(color.b)
        // lightsBuffer[4] = color.r
        // lightsBuffer[5] = color.g
        // lightsBuffer[6] = color.b
    }

    fun dirIntensity(intensity: Float) {
        lightsBuffer.position = 7 * Float.SIZE_BYTES
        lightsBuffer.putFloat(intensity)
        // lightsBuffer[7] = intensity
    }

    fun dirDirection(direction: Vec3f) {
        lightsBuffer.position = 8 * Float.SIZE_BYTES
        lightsBuffer.putFloat(direction.x)
        lightsBuffer.putFloat(direction.y)
        lightsBuffer.putFloat(direction.z)
        //        lightsBuffer[8] = direction.x
        //        lightsBuffer[9] = direction.y
        //        lightsBuffer[10] = direction.z
    }

    fun pointLight(index: Int, position: Vec3f, color: Color, intensity: Float, range: Float) {
        if (index >= maxLightCount) {
            return
        }
        if (intensity > 0) {
            val offset = (POINT_LIGHT_OFFSET * index * 4) + POINT_LIGHT_OFFSET * 4
            lightsBuffer.position = offset
            lightsBuffer.putFloat(position.x)
            lightsBuffer.putFloat(position.y)
            lightsBuffer.putFloat(position.z)
            lightsBuffer.putFloat(range)
            lightsBuffer.putFloat(color.r)
            lightsBuffer.putFloat(color.g)
            lightsBuffer.putFloat(color.b)
            lightsBuffer.putFloat(intensity)
            // lightsBuffer[offset] = position.x
            // lightsBuffer[offset + 1] = position.y
            // lightsBuffer[offset + 2] = position.z
            // lightsBuffer[offset + 3] = range
            // lightsBuffer[offset + 4] = color.r
            // lightsBuffer[offset + 5] = color.g
            // lightsBuffer[offset + 6] = color.b
            // lightsBuffer[offset + 7] = intensity
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
        // lightsBuffer[11] = 0f
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
