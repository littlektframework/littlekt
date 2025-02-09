package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
abstract class MainShaderBlock(
    val entryPoint: String,
    structs: Set<ShaderStruct>,
    bindingGroups: Set<ShaderBindGroup>,
    blocks: List<String>,
    rules: List<ShaderBlockInsertRule>,
    body: String,
) : ShaderBlock(structs, bindingGroups, blocks, rules, body)
