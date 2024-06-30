package com.littlekt.resources

import com.littlekt.EngineStats
import com.littlekt.graphics.webgpu.WebGPUTexture

/**
 * @author Colton Daily
 * @date 4/14/2024
 */
class TextureResourceInfo(val texture: WebGPUTexture, val size: Long = 0L) : ResourceInfo() {
    init {
        EngineStats.textureAllocated(this)
    }

    override fun delete() {
        EngineStats.textureDeleted(id)
    }
}
