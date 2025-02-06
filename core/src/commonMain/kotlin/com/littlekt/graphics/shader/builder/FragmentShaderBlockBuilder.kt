package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class FragmentShaderBlockBuilder(base: ShaderBlock? = null) : ShaderBlockBuilder(base) {
    fun main(
        input: ShaderStruct,
        output: ShaderStruct,
        entry: String = "main",
        inputVar: String = "input",
        block: ShaderBlockBuilder.() -> String,
    ) {
        body =
            "%fragment_start%\n@fragment fn $entry($inputVar: ${input.name}) -> ${output.name} {\n${block()}\n}\n%fragment_end%"
    }
}
