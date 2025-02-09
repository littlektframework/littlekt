package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor

/**
 * @author Colton Daily
 * @date 2/7/2025
 */
open class ShaderBlockBindGroup(
    val group: Int,
    val bindingUsage: BindingUsage,
    val descriptor: BindGroupLayoutDescriptor,
    body: String,
) : ShaderBlock(emptySet(), emptySet(), emptyList(), emptyList(), body)
