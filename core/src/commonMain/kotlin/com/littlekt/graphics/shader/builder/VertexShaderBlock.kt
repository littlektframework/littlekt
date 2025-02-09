package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
open class VertexShaderBlock(
    entryPoint: String,
    structs: Set<ShaderStruct>,
    bindingGroups: Set<ShaderBindGroup>,
    blocks: List<String>,
    rules: List<ShaderBlockInsertRule>,
    body: String,
) : MainShaderBlock(entryPoint, structs, bindingGroups, blocks, rules, body)
