package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor

fun shader(shader: ShaderCode? = null, block: ShaderCodeBuilder.() -> Unit): ShaderCode {
    val builder = ShaderCodeBuilder(shader)
    builder.block()
    return builder.build()
}

fun shaderBlock(base: ShaderBlock? = null, block: ShaderBlockBuilder.() -> Unit): ShaderBlock {
    val builder = ShaderBlockBuilder(base)
    builder.block()
    return builder.build()
}

fun shaderBindGroup(
    group: Int,
    bindingUsage: BindingUsage,
    descriptor: BindGroupLayoutDescriptor,
    body: () -> String,
): ShaderBlockBindGroup {
    return ShaderBlockBindGroup(group, bindingUsage, descriptor, body())
}

fun shaderStructOld(name: String, body: () -> String) =
    ShaderBlock(emptySet(), emptySet(), emptyList(), emptyList(), body())

fun shaderStruct(name: String, body: () -> Map<String, ShaderStructParameterType>): ShaderStruct {
    return ShaderStruct(name, body())
}
