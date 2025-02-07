package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class MainShaderBlock(
    val entryPoint: String,
    type: ShaderBlockType,
    includes: List<ShaderBlock>,
    rules: List<ShaderBlockInsertRule>,
    body: String,
) : ShaderBlock(type, includes, rules, body)
