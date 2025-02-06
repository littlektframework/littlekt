package com.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderBlock(
    val includes: List<ShaderStruct>,
    val markers: Map<String, String>,
    val body: String,
) {
    val src by lazy {
        buildString {
            includes.forEach { appendLine(it.src) }
            appendLine(body)
        }
    }
}
