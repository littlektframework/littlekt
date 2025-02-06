package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class VertexShaderBlockBuilder(base: ShaderBlock? = null) : ShaderBlockBuilder(base) {
    override var type = ShaderBlockType.VERTEX

    fun main(
        input: ShaderStruct,
        output: ShaderStruct,
        entry: String = "main",
        inputVar: String = "input",
        block: ShaderBlockBuilder.() -> String,
    ) {
        body =
            "%vertex_start%\n@vertex fn $entry($inputVar: ${input.name}) -> ${output.name} {\n${block()}\n}\n%vertex_end%"
    }
}
