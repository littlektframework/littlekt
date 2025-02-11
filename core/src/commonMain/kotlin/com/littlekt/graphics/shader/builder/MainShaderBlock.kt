package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
abstract class MainShaderBlock(
    val entryPoint: String,
    name: String,
    structs: Set<ShaderStruct>,
    bindingGroups: Set<ShaderBindGroup>,
    blocks: Set<ShaderBlock>,
    rules: List<ShaderBlockInsertRule>,
    body: String,
) : ShaderBlock(name, structs, bindingGroups, blocks, rules, body)
