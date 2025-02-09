package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class FragmentShaderBlockBuilder(base: FragmentShaderBlock? = null) :
    MainShaderBlockBuilder<FragmentShaderBlock>(FragmentShaderBlock.BLOCK_NAME, base) {

    override var entry: String = "fs_main"

    fun main(
        input: ShaderStruct,
        output: ShaderStruct,
        entry: String = this.entry,
        inputVar: String = "input",
        block: ShaderBlockBuilder.() -> String,
    ) {
        this.entry = entry
        body =
            "%fragment_start%\n@fragment fn $entry($inputVar: ${input.name}) -> ${output.name} {\n${block()}\n}\n%fragment_end%"
    }

    override fun build(extraStructs: Set<ShaderStruct>): FragmentShaderBlock {
        structs.addAll(extraStructs)
        return FragmentShaderBlock(entry, structs, bindingGroups, blocks, rules, body)
    }
}
