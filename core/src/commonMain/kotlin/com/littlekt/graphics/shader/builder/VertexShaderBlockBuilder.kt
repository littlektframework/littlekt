package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class VertexShaderBlockBuilder(base: VertexShaderBlock? = null) :
    MainShaderBlockBuilder<VertexShaderBlock>(base) {

    fun main(
        input: ShaderStruct,
        output: ShaderStruct,
        entry: String = "main",
        inputVar: String = "input",
        block: ShaderBlockBuilder.() -> String,
    ) {
        this.entry = entry
        body =
            "%vertex_start%\n@vertex fn $entry($inputVar: ${input.name}) -> ${output.name} {\n${block()}\n}\n%vertex_end%"
    }

    override fun build(): VertexShaderBlock {
        return VertexShaderBlock(entry, structs, bindingGroups, blocks, rules, body)
    }
}
