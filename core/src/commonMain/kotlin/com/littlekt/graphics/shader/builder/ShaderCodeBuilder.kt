package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderCodeBuilder(base: ShaderCode? = null) {
    private val vertexBase = base?.vertex
    private val fragmentBase = base?.fragment
    private val computeBase = base?.compute
    private var vertex: VertexShaderBlock? = null
    private var fragment: FragmentShaderBlock? = null
    private var compute: ComputeShaderBlock? = null
    private var vertexBuilder: VertexShaderBlockBuilder? = null
    private var fragmentBuilder: FragmentShaderBlockBuilder? = null
    private var computeBuilder: ComputeShaderBlockBuilder? = null

    private val structs = mutableSetOf<ShaderStruct>().apply { base?.structs?.let { addAll(it) } }
    private val bindingGroups =
        mutableSetOf<ShaderBindGroup>().apply { base?.bindingGroups?.let { addAll(it) } }
    private val blocks = mutableListOf<String>().apply { base?.let { addAll(it.blocks) } }
    private val rules =
        mutableListOf<ShaderBlockInsertRule>().apply { base?.let { addAll(it.rules) } }

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

    fun include(block: ShaderBlockBuilder.() -> Unit) {
        val builder = ShaderBlockBuilder()
        builder.block()
        include(builder.build())
    }

    fun vertex(base: VertexShaderBlock? = null, block: VertexShaderBlockBuilder.() -> Unit = {}) {
        val builder = VertexShaderBlockBuilder(base ?: vertexBase)
        builder.block()
        vertexBuilder = builder
    }

    fun fragment(
        base: FragmentShaderBlock? = null,
        block: FragmentShaderBlockBuilder.() -> Unit = {},
    ) {
        val builder = FragmentShaderBlockBuilder(base ?: fragmentBase)
        builder.block()
        fragmentBuilder = builder
    }

    fun compute(
        base: ComputeShaderBlock? = null,
        block: ComputeShaderBlockBuilder.() -> Unit = {},
    ) {
        val builder = ComputeShaderBlockBuilder(base ?: computeBase)
        builder.block()
        computeBuilder = builder
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

    fun build(): ShaderCode {
        ensureMainBlockBuilders()
        buildMainShaderBlocks()
        return ShaderCode(structs, bindingGroups, blocks, rules, vertex, fragment, compute)
    }

    /** Ensure we have builders if we have any base. */
    private fun ensureMainBlockBuilders() {
        vertexBuilder = vertexBuilder ?: vertexBase?.let { VertexShaderBlockBuilder(it) }
        fragmentBuilder = fragmentBuilder ?: fragmentBase?.let { FragmentShaderBlockBuilder(it) }
        computeBuilder = computeBuilder ?: computeBase?.let { ComputeShaderBlockBuilder(it) }
    }

    private fun buildMainShaderBlocks() {
        // build the intermediate main shader blocks to collect all the structs
        val vertexInter =
            vertexBuilder?.build()?.also {
                structs.addAll(it.structs)
                rules.addAll(it.rules)
                bindingGroups.addAll(it.bindingGroups)
            }
        val fragmentInter =
            fragmentBuilder?.build()?.also {
                structs.addAll(it.structs)
                rules.addAll(it.rules)
                bindingGroups.addAll(it.bindingGroups)
            }
        val computeInter =
            computeBuilder?.build()?.also {
                structs.addAll(it.structs)
                rules.addAll(it.rules)
                bindingGroups.addAll(it.bindingGroups)
            }

        // rebuild them and add all the structs the shader has included into them as since we can
        // extend them, and it's nicer to not have to include everything by hand if it's been
        // declared
        // at the top level

        vertex =
            vertexInter?.let {
                VertexShaderBlock(
                    it.entryPoint,
                    structs,
                    bindingGroups,
                    it.blocks,
                    it.rules,
                    it.body,
                )
            }

        fragment =
            fragmentInter?.let {
                FragmentShaderBlock(
                    it.entryPoint,
                    structs,
                    bindingGroups,
                    it.blocks,
                    it.rules,
                    it.body,
                )
            }

        compute =
            computeInter?.let {
                ComputeShaderBlock(
                    it.entryPoint,
                    structs,
                    bindingGroups,
                    it.blocks,
                    it.rules,
                    it.body,
                )
            }
    }
}
