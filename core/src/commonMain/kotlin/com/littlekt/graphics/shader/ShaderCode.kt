package com.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCode(val includes: List<ShaderStruct>, val blocks: List<ShaderBlock>) {
    val src by lazy {
        buildString {
            includes.forEach { appendLine(it.src) }
            blocks.forEach { appendLine(it.src) }
        }
    }
}
