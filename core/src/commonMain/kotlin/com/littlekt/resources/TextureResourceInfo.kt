package com.littlekt.resources

import com.littlekt.EngineStats
import io.ygdrasil.webgpu.Texture

/**
 * @author Colton Daily
 * @date 4/14/2024
 */
class TextureResourceInfo(val texture: Texture, val size: Long = 0L) : ResourceInfo() {
    init {
        EngineStats.textureAllocated(this)
    }

    override fun delete() {
        EngineStats.textureDeleted(id)
    }
}
