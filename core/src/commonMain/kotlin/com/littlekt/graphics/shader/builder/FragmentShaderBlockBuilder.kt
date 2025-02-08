package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class FragmentShaderBlockBuilder(base: ShaderBlock? = null) : MainShaderBlockBuilder(base) {
    override var type = ShaderBlockType.FRAGMENT

    fun main(
        input: ShaderBlock,
        output: ShaderBlock,
        entry: String = "main",
        inputVar: String = "input",
        block: ShaderBlockBuilder.() -> String,
    ) {
        this.entry = entry
        body =
            "%fragment_start%\n@fragment fn $entry($inputVar: ${input}) -> ${output} {\n${block()}\n}\n%fragment_end%"
    }
}
