package com.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderCodeBuilder(base: ShaderCode? = null) {
    private val includes =
        mutableListOf<ShaderStruct>().apply { base?.includes?.let { addAll(it) } }
    private val blocks = mutableListOf<ShaderBlock>().apply { base?.let { addAll(it.blocks) } }

    fun include(struct: ShaderStruct) {
        includes.add(struct)
    }

    fun vertex(base: ShaderBlock? = null, block: VertexShaderBlockBuilder.() -> Unit) {
        val builder = VertexShaderBlockBuilder(base)
        builder.block()
        blocks.add(builder.build())
    }

    fun fragment(base: ShaderBlock? = null, block: FragmentShaderBlockBuilder.() -> Unit) {
        val builder = FragmentShaderBlockBuilder(base)
        builder.block()
        blocks.add(builder.build())
    }

    fun build(): ShaderCode {
        return ShaderCode(includes, blocks)
    }
}
