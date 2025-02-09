package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
open class VertexShaderBlock(
    entryPoint: String,
    structs: Set<ShaderStruct>,
    bindingGroups: Set<ShaderBindGroup>,
    blocks: Set<ShaderBlock>,
    rules: List<ShaderBlockInsertRule>,
    body: String,
) : MainShaderBlock(entryPoint, BLOCK_NAME, structs, bindingGroups, blocks, rules, body) {
    companion object {
        const val BLOCK_NAME = "VERTEX"
    }
}
