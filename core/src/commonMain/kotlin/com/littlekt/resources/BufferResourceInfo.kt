package com.littlekt.resources

import com.littlekt.EngineStats
import com.littlekt.graphics.webgpu.GPUBuffer

/**
 * @author Colton Daily
 * @date 4/14/2024
 */
class BufferResourceInfo(val buffer: GPUBuffer, val size: Long = 0L) : ResourceInfo() {
    init {
        EngineStats.bufferAllocated(this)
    }

    override fun delete() {
        EngineStats.bufferDeleted(id)
    }
}
