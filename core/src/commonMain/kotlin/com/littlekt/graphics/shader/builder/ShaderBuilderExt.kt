package com.littlekt.graphics.shader.builder

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

fun shaderStruct(name: String, body: () -> Map<String, ShaderStructParameterType>): ShaderStruct {
    return ShaderStruct(name, body())
}
