package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
abstract class MainShaderBlockBuilder<T : MainShaderBlock>(base: T? = null) :
    ShaderBlockBuilder(base) {
    protected var entry: String = "main"

    abstract fun build(extraStructs: Set<ShaderStruct>): T

    override fun build(): T = build(emptySet())
}
