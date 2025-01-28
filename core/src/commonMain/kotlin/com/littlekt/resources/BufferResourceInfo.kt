package com.littlekt.resources

import com.littlekt.EngineStats
import io.ygdrasil.webgpu.Buffer

/**
 * @author Colton Daily
 * @date 4/14/2024
 */
class BufferResourceInfo(val buffer: Buffer, val size: Long = 0L) : ResourceInfo() {
    init {
        EngineStats.bufferAllocated(this)
    }

    override fun delete() {
        EngineStats.bufferDeleted(id)
    }
}
