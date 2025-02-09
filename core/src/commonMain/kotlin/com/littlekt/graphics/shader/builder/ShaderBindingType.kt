package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
sealed class ShaderBindingType(val name: String) {
    /**
     * A storage type. Its name is based off of [access]. `<storage, read>` `<storage, read_write>`
     * `<storage, write>`
     */
    data class Storage(val access: MemoryAccessMode) : ShaderBindingType("<storage, $access>")

    /** A uniform type. Its name is `<uniform>`. */
    data object Uniform : ShaderBindingType("<uniform>")

    /**
     * "Plain" or "Empty". Its name is an empty string. This is for usages with things like
     * texture_2d and sampler.
     */
    data object Plain : ShaderBindingType("")
}
