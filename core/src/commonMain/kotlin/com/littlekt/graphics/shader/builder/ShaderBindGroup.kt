package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor
import com.littlekt.graphics.webgpu.ShaderStage

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

    fun generateBindGroupLayoutDescriptor(visibility: ShaderStage): BindGroupLayoutDescriptor {
        return BindGroupLayoutDescriptor(bindings.map { it.generateBindingLayoutEntry(visibility) })
    }
}
