package com.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class VertexShaderBlockBuilder(base: ShaderBlock? = null) : ShaderBlockBuilder(base) {
    fun main(
        input: ShaderStruct,
        output: ShaderStruct,
        name: String = "main",
        block: ShaderBlockBuilder.() -> String,
    ) {
        body =
            "@vertex fn $name(${input.name.toSnakeCase()}: ${input.name}) -> ${output.name} {\n${block()}\n}"
    }
}
