package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.BufferBinding
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.Angle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A class for managing GPU light data in a buffer, supporting different types of lights such as
 * ambient, directional, point and spot lights. The data is stored as a structured collection of
 * floats and can be updated and sent to the GPU.
 *
 * @property device The device used for creating GPU buffers and managing the queue.
 * @property maxLightCount The maximum number of point & spot lights this buffer can store.
 */
class LightBuffer(val device: Device, val maxLightCount: Int) : Releasable {
    /** Size of light buffer in number of components / floats. */
    private val lightBufferSize =
        AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE + (LIGHT_SIZE * maxLightCount)
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

    /** The number of point & spot lights currently in the buffer. */
    var lightCount: Int = 0
        private set

    fun update() {
        lightsBuffer.position = LIGHT_COUNT_OFFSET * Float.SIZE_BYTES
        lightsBuffer.putInt(lightCount)
        val size = (LIGHT_OFFSET + lightCount * LIGHT_SIZE) * Float.SIZE_BYTES
        device.queue.writeBuffer(buffer, lightsBuffer, size = size.toLong())
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

    /**
     * Adds a point light to the end of the buffer. Lights without any intensity are skipped.
     *
     * @return `true` if the light was added.
     */
    fun pointLight(position: Vec3f, color: Color, intensity: Float, range: Float): Boolean {
        if (!beginLight(position, color, intensity, range)) return false
        lightsBuffer.putFloat(0f)
        lightsBuffer.putFloat(0f)
        lightsBuffer.putFloat(0f)
        lightsBuffer.putInt(-1)
        lightsBuffer.putFloat(0f)
        lightsBuffer.putFloat(1f)
        lightCount++
        return true
    }

    /**
     * Adds a spot light to the end of the buffer. Lights without any intensity are skipped.
     *
     * @param shadowIndex the layer of the light in the spot shadow map; `-1` if it has none.
     * @return `true` if the light was added.
     */
    fun spotLight(
        position: Vec3f,
        direction: Vec3f,
        color: Color,
        intensity: Float,
        range: Float,
        innerAngle: Angle,
        outerAngle: Angle,
        shadowIndex: Int = -1,
    ): Boolean {
        if (!beginLight(position, color, intensity, range)) return false
        val length =
            sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z)
        val invLength = if (length > 0f) 1f / length else 0f
        val cosOuter = cos(outerAngle.radians)
        val cosInner = max(cos(innerAngle.radians), cosOuter)
        val scale = 1f / max(cosInner - cosOuter, 0.001f)
        lightsBuffer.putFloat(direction.x * invLength)
        lightsBuffer.putFloat(direction.y * invLength)
        lightsBuffer.putFloat(direction.z * invLength)
        lightsBuffer.putInt(shadowIndex)
        lightsBuffer.putFloat(scale)
        lightsBuffer.putFloat(-cosOuter * scale)
        lightCount++
        return true
    }

    private fun beginLight(position: Vec3f, color: Color, intensity: Float, range: Float): Boolean {
        if (lightCount >= maxLightCount || intensity <= 0f) return false
        lightsBuffer.position = (LIGHT_OFFSET + lightCount * LIGHT_SIZE) * Float.SIZE_BYTES
        lightsBuffer.putFloat(position.x)
        lightsBuffer.putFloat(position.y)
        lightsBuffer.putFloat(position.z)
        lightsBuffer.putFloat(range)
        lightsBuffer.putFloat(color.r)
        lightsBuffer.putFloat(color.g)
        lightsBuffer.putFloat(color.b)
        lightsBuffer.putFloat(intensity)
        return true
    }

    fun resetLightCount() {
        lightCount = 0
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val AMBIENT_LIGHT_SIZE = 4

        /** Size in total floats (NOT BYTE SIZE). */
        const val DIRECTIONAL_LIGHT_SIZE = 8

        /** Size of a point or spot light in total floats (NOT BYTE SIZE). */
        const val LIGHT_SIZE = 16

        private const val LIGHT_COUNT_OFFSET = 11
        private const val LIGHT_OFFSET = AMBIENT_LIGHT_SIZE + DIRECTIONAL_LIGHT_SIZE
    }

    override fun release() {
        buffer.release()
    }
}
