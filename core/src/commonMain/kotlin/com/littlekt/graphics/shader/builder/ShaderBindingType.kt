package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
sealed class ShaderBindingType(val name: String) {
    data class Storage(val access: MemoryAccessMode) : ShaderBindingType("<storage, $access>")

    data object Uniform : ShaderBindingType("<uniform>")

    data object Empty : ShaderBindingType("")
}
