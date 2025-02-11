package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
abstract class MainShaderBlockBuilder<T : MainShaderBlock>(name: String, base: T? = null) :
    ShaderBlockBuilder(name, base) {
    abstract var entry: String

    abstract fun build(extraStructs: Set<ShaderStruct>): T

    override fun build(): T = build(emptySet())
}
