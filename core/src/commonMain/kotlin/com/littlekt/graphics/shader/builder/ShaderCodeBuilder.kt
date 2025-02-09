package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderCodeBuilder(base: ShaderCode? = null) {
    private var vertexBase = base?.vertex
    private var fragmentBase = base?.fragment
    private var computeBase = base?.compute
    private val structs = mutableSetOf<ShaderStruct>().apply { base?.structs?.let { addAll(it) } }
    private val bindingGroups =
        mutableSetOf<ShaderBindGroup>().apply { base?.bindingGroups?.let { addAll(it) } }
    private val blocks = mutableListOf<String>().apply { base?.let { addAll(it.blocks) } }
    private val rules = mutableListOf<ShaderBlockInsertRule>()

    fun include(struct: ShaderStruct) {
        structs.add(struct)
    }

    fun include(block: ShaderBlock) {
        structs.addAll(block.structs)
        bindingGroups.addAll(block.bindingGroups)
        rules.addAll(block.rules)
        blocks.add(block.body)
    }

    fun include(block: ShaderBlockBuilder.() -> Unit) {
        val builder = ShaderBlockBuilder()
        builder.block()
        include(builder.build())
    }

    fun vertex(base: VertexShaderBlock? = null, block: VertexShaderBlockBuilder.() -> Unit) {
        val builder = VertexShaderBlockBuilder(base ?: vertexBase)
        builder.block()
        vertexBase = builder.build()
    }

    fun fragment(base: FragmentShaderBlock? = null, block: FragmentShaderBlockBuilder.() -> Unit) {
        val builder = FragmentShaderBlockBuilder(base ?: fragmentBase)
        builder.block()
        fragmentBase = builder.build()
    }

    fun compute(base: ComputeShaderBlock? = null, block: ComputeShaderBlockBuilder.() -> Unit) {
        val builder = ComputeShaderBlockBuilder(base ?: computeBase)
        builder.block()
        computeBase = builder.build()
    }

    fun build(): ShaderCode {
        vertexBase?.let { include(it) }
        fragmentBase?.let { include(it) }
        computeBase?.let { include(it) }
        return ShaderCode(
            structs,
            bindingGroups,
            blocks,
            rules,
            vertexBase,
            fragmentBase,
            computeBase,
        )
    }
}
