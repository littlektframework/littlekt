package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.util.datastructure.fastForEach
import kotlin.math.max

/**
 * A buffers helper class for handling instancing data. When resized, the old buffer is kept until
 * [releasePending] is called.
 *
 * @param instanceDataSize the initial size of the instance data as number of components (NOT
 *   BYTES)
 * @author Colton Daily
 * @date 1/15/2025
 */
class InstanceBuffers(val device: Device, instanceDataSize: Int) : Releasable {

    /** The current capacity of the storage buffer as number of components (NOT BYTES). */
    var capacity: Int = max(instanceDataSize, MIN_CAPACITY)
        private set

    private var storage = createStorage(capacity)
    private var storageBinding = BufferBinding(storage)

    private var bindGroup: BindGroup? = null
    private val pendingRelease = mutableListOf<Releasable>()

    /**
     * @return an existing bind group, created previously, if not then creates the bind group for
     *   the instance.
     */
    fun getOrCreateBindGroup(layout: BindGroupLayout): BindGroup {
        return bindGroup
            ?: device
                .createBindGroup(
                    BindGroupDescriptor(layout, listOf(BindGroupEntry(0, storageBinding)))
                )
                .also { bindGroup = it }
    }

    /**
     * Ensures the storage buffer can hold at least [size] components. If it can't, a new buffer is
     * created and the old buffer and bind group are released on the next [releasePending].
     *
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun ensureCapacity(size: Int): Boolean {
        if (size <= capacity) return false
        logger.debug {
            "Instance storage buffer has run out of room. Increasing size to a size of ${max(size, capacity * 2)}"
        }
        capacity = max(size, capacity * 2)
        pendingRelease += storage
        bindGroup?.let { pendingRelease += it }
        bindGroup = null
        storage = createStorage(capacity)
        storageBinding = BufferBinding(storage)
        return true
    }

    /**
     * Writes [len] components of [data], starting at index `0`, into the storage buffer at
     * [offset]. [ensureCapacity] should be called beforehand.
     *
     * @param offset the offset, as number of components, of the storage buffer to write to.
     */
    fun write(data: FloatBuffer, offset: Int, len: Int) {
        if (len <= 0) return
        device.queue.writeBuffer(
            storage,
            data,
            offset = offset.toLong() * Float.SIZE_BYTES,
            size = len.toLong(),
        )
    }

    /** Releases any old buffers that were replaced via [ensureCapacity]. */
    fun releasePending() {
        if (pendingRelease.isEmpty()) return
        pendingRelease.fastForEach { it.release() }
        pendingRelease.clear()
    }

    private fun createStorage(size: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "instance buffers storage buffer",
                size.toLong() * Float.SIZE_BYTES,
                BufferUsage.STORAGE or BufferUsage.COPY_DST,
                false,
            )
        )

    override fun release() {
        releasePending()
        storage.release()
        bindGroup?.release()
    }

    companion object {
        private const val MIN_CAPACITY = 20
        private val logger = Logger<InstanceBuffers>()
    }
}
