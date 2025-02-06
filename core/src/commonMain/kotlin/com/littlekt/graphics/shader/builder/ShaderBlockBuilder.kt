package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderBlockBuilder(base: ShaderBlock? = null) {
    val includes = mutableListOf<ShaderBlock>().apply { base?.includes?.let { addAll(it) } }
    val rules = mutableListOf<ShaderBlockInsertRule>().apply { base?.rules?.let { addAll(it) } }
    var body: String = base?.body ?: ""

    fun include(block: ShaderBlock) {
        includes.add(block)
    }

    fun body(block: () -> String) {
        body = block()
    }

    fun before(marker: String, block: ShaderBlock) {
        insert(ShaderBlockInsertType.BEFORE, marker, block)
    }

    fun before(marker: String, block: ShaderBlockBuilder.() -> Unit) {
        val builder = ShaderBlockBuilder()
        builder.block()
        insert(ShaderBlockInsertType.BEFORE, marker, builder.build())
    }

    fun after(marker: String, block: ShaderBlock) {
        insert(ShaderBlockInsertType.AFTER, marker, block)
    }

    fun after(marker: String, block: ShaderBlockBuilder.() -> Unit) {
        val builder = ShaderBlockBuilder()
        builder.block()
        insert(ShaderBlockInsertType.AFTER, marker, builder.build())
    }

    private fun insert(type: ShaderBlockInsertType, marker: String, block: ShaderBlock) =
        rules.add(ShaderBlockInsertRule(type, marker, block))

    fun build(): ShaderBlock {
        return ShaderBlock(includes, rules, body)
    }
}
