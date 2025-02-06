package com.littlekt.graphics.shader

fun shader(shader: ShaderCode? = null, block: ShaderCodeBuilder.() -> Unit): ShaderCode {
    val builder = ShaderCodeBuilder(shader)
    builder.block()
    return builder.build()
}

fun shaderStruct(name: String, body: () -> String): ShaderStruct {
    return ShaderStruct(name, body())
}
