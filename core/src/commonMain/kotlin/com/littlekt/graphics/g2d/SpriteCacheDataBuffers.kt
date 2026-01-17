package com.littlekt.graphics.g2d

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.BufferBinding
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPUBuffer
import com.littlekt.log.Logger
import kotlin.math.min

class SpriteCacheDataBuffers(val device: Device, staticSize: Int, dynamicSize: Int) : SpriteCacheData {
    /**
     * The [GPUBuffer] that holds the static sprite data.
     *
     * @see updateSpriteStaticStorage
     */
    private var spriteStaticStorage =
        device.createGPUFloatBuffer(
            "static sprite storage buffer",
            FloatArray(staticSize),
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    var staticSpriteStorageBufferBinding = BufferBinding(spriteStaticStorage)
        private set

    /**
     * The [GPUBuffer] that holds the dynamic sprite data.
     *
     * @see updateSpriteStaticStorage
     */
    private var spriteDynamicStorage =
        device.createGPUFloatBuffer(
            "dynamic sprite storage buffer",
            FloatArray(dynamicSize),
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    var dynamicSpriteStorageBufferBinding = BufferBinding(spriteDynamicStorage)
        private set

    /**
     * Update this [spriteStaticStorage] with the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    override fun updateSpriteStaticStorage(data: FloatBuffer): Boolean {
        if (spriteStaticStorage.size < data.capacity * Float.SIZE_BYTES) {
            logger.debug {
                "Attempting to write data to static sprite storage buffer that exceeds its current size. Destroying and recreating the buffer..."
            }
            spriteStaticStorage.release()
            spriteStaticStorage =
                device.createGPUFloatBuffer(
                    "static sprite storage buffer",
                    data,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                )
            staticSpriteStorageBufferBinding = BufferBinding(spriteStaticStorage)
            return true
        } else {
            device.queue.writeBuffer(
                spriteStaticStorage,
                data,
                size = min(spriteStaticStorage.size / Float.SIZE_BYTES, data.limit.toLong()),
            )
        }
        return false
    }

    /**
     * Update this [spriteDynamicStorage] with the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    override fun updateSpriteDynamicStorage(data: FloatBuffer): Boolean {
        if (spriteDynamicStorage.size < data.capacity * Float.SIZE_BYTES) {
            logger.debug {
                "Attempting to write data to dynamic sprite storage buffer that exceeds its current size. Destroying and recreating the buffer..."
            }
            spriteDynamicStorage.release()
            spriteDynamicStorage =
                device.createGPUFloatBuffer(
                    "dynamic sprite storage buffer",
                    data,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                )
            dynamicSpriteStorageBufferBinding = BufferBinding(spriteDynamicStorage)
            return true
        } else {
            device.queue.writeBuffer(
                spriteDynamicStorage,
                data,
                size = min(spriteDynamicStorage.size / Float.SIZE_BYTES, data.limit.toLong()),
            )
        }
        return false
    }

    override fun release() {
        spriteStaticStorage.release()
        spriteDynamicStorage.release()
    }

    companion object {
        private val logger = Logger<SpriteCacheDataBuffers>()
    }
}