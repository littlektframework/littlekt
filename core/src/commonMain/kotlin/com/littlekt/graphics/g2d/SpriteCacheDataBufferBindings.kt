package com.littlekt.graphics.g2d

import com.littlekt.Releasable
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroup

class SpriteCacheDataBufferBindings(val buffers: SpriteCacheDataBuffers) : Releasable{

    private var bindGroups = mutableMapOf<Int, BindGroup>()

    val bindingUsage: BindingUsage = SPRITE_STORAGE

    fun getOrCreateBindGroup(shader: Shader): BindGroup {
        return bindGroups[shader.id]
            ?: shader
                .createBindGroup(
                    bindingUsage,
                    buffers.staticSpriteStorageBufferBinding,
                    buffers.dynamicSpriteStorageBufferBinding
                )
                ?.also { bindGroups[shader.id] = it }
            ?: error("Unable to create bind group for shader: ${shader.id} for $bindingUsage!")
    }

    fun releaseBindGroup(shader: Shader) {
        bindGroups.remove(shader.id)?.release()
    }

    override fun release() {
        bindGroups.values.forEach { it.release() }
    }

    companion object {
        val SPRITE_STORAGE = BindingUsage("SpriteCache Sprite Storage")
    }
}