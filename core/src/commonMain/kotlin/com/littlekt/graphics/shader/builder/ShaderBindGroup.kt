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
) : ShaderSrc() {
    override val src by lazy { buildString { bindings.forEach { appendLine(it.src) } }.format() }
}
