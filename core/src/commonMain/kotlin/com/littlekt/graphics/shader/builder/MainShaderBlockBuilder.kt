package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
abstract class MainShaderBlockBuilder<T : MainShaderBlock>(base: T? = null) :
    ShaderBlockBuilder(base) {
    protected var entry: String = "main"

    override fun build(): T = error("Implement build!")
}
