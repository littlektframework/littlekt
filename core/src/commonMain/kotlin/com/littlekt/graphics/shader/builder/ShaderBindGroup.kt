package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
data class ShaderBindGroup(
    val group: Int,
    val usage: BindingUsage,
    val bindings: List<ShaderBinding>,
) {
    val src by lazy { bindings.forEach {} }
}
