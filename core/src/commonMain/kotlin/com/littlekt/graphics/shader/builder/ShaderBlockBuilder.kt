package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderBlockBuilder(base: ShaderBlock? = null) {
    protected val structs = mutableSetOf<ShaderStruct>().apply { base?.structs?.let { addAll(it) } }
    protected val bindingGroups =
        mutableSetOf<ShaderBindGroup>().apply { base?.bindingGroups?.let { addAll(it) } }
    protected val blocks = mutableListOf<String>().apply { base?.let { addAll(it.blocks) } }
    protected val rules =
        mutableListOf<ShaderBlockInsertRule>().apply { base?.let { addAll(it.rules) } }
    protected var body = base?.body ?: ""

    fun bindGroup(bindGroup: ShaderBindGroup) {
        bindingGroups.add(bindGroup)
    }

    fun bindGroup(group: Int, usage: BindingUsage, block: ShaderBindGroupBuilder.() -> Unit) {
        val builder = ShaderBindGroupBuilder(group, usage)
        builder.block()
        bindingGroups.add(builder.build())
    }

    fun include(struct: ShaderStruct) {
        structs.add(struct)
    }

    fun include(block: ShaderBlock) {
        check(block !is VertexShaderBlock) {
            "You may not include a VertexShaderBlock! Use 'vertex(vertexBlock) {}' instead!"
        }
        check(block !is FragmentShaderBlock) {
            "You may not include a FragmentShaderBlock! Use 'fragment(fragmentBlock) {}' instead!"
        }
        check(block !is ComputeShaderBlock) {
            "You may not include a ComputeShaderBlock! Use 'compute(computeBlock) {}' instead!"
        }
        structs.addAll(block.structs)
        bindingGroups.addAll(block.bindingGroups)
        rules.addAll(block.rules)
        blocks.add(block.body)
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

    open fun build(): ShaderBlock {
        return ShaderBlock(structs, bindingGroups, blocks, rules, body)
    }
}
