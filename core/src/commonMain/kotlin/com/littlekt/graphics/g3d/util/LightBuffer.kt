package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class LightBuffer(val device: Device, val maxLightCount: Int) : Releasable {
    /** Size of light buffer in number of components / floats. */
    private val lightBufferSize =
        AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE + (POINT_LIGHT_SIZE * maxLightCount)
    private val lightBufferByteSize = lightBufferSize * Float.SIZE_BYTES
    private val lightsBuffer = FloatBuffer(lightBufferSize)

    /** The [GPUBuffer] that holds the light data */
    val buffer = run {
        val buffer =
            device.createBuffer(
                BufferDescriptor(
                    "light",
                    lightBufferByteSize.toLong(),
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                    true,
                )
            )
        buffer.getMappedRange().putFloat(lightsBuffer)
        buffer.unmap()

        buffer
    }

    /** The [BufferBinding] for [buffer]. */
    val bufferBinding = BufferBinding(buffer, size = lightBufferByteSize.toLong())

    fun update() {
        device.queue.writeBuffer(buffer, lightsBuffer)
    }

    fun ambient(color: Color) {
        lightsBuffer[0] = color.r
        lightsBuffer[1] = color.g
        lightsBuffer[2] = color.b
        lightsBuffer[3] = 0f // padding
    }

    fun dirColor(color: Color) {
        lightsBuffer[4] = color.r
        lightsBuffer[5] = color.g
        lightsBuffer[6] = color.b
    }

    fun dirIntensity(intensity: Float) {
        lightsBuffer[7] = intensity
    }

    fun dirDirection(direction: Vec3f) {
        lightsBuffer[8] = direction.x
        lightsBuffer[9] = direction.y
        lightsBuffer[10] = direction.z
    }

    fun pointLight(index: Int, position: Vec3f, color: Color, intensity: Float, range: Float) {
        require(intensity >= 1) { "Point Light index must be >= 1" }
        if (index >= maxLightCount) {
            return
        }
        if (intensity > 0) {
            val offset = POINT_LIGHT_OFFSET * index
            lightsBuffer[offset] = position.x
            lightsBuffer[offset + 1] = position.y
            lightsBuffer[offset + 2] = position.z
            lightsBuffer[offset + 3] = range
            lightsBuffer[offset + 4] = color.r
            lightsBuffer[offset + 5] = color.g
            lightsBuffer[offset + 6] = color.b
            lightsBuffer[offset + 7] = intensity
            incrementLightCount()
        }
    }

    private fun incrementLightCount() {
        lightsBuffer[11] += 1f
    }

    fun resetLightCount() {
        lightsBuffer[11] = 0f
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
