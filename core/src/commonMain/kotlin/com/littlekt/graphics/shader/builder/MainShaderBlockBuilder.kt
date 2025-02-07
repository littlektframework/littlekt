package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class MainShaderBlockBuilder(base: ShaderBlock? = null) : ShaderBlockBuilder(base) {
    protected var entry: String = "main"

    override fun build(): ShaderBlock {
        return MainShaderBlock(entry, type, includes, rules, body)
    }
}
