package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderCodeBuilder(base: ShaderCode? = null) {
    private val vertexBase = base?.blocks?.firstOrNull { it.type == ShaderBlockType.VERTEX }
    private val fragmentBase = base?.blocks?.firstOrNull { it.type == ShaderBlockType.FRAGMENT }
    private val computeBase = base?.blocks?.firstOrNull { it.type == ShaderBlockType.COMPUTE }
    private val includes = mutableListOf<ShaderBlock>().apply { base?.includes?.let { addAll(it) } }
    private val blocks = mutableListOf<ShaderBlock>().apply { base?.let { addAll(it.blocks) } }

    fun include(block: ShaderBlock) {
        includes.add(block)
    }

    fun include(block: ShaderBlockBuilder.() -> Unit) {
        val builder = ShaderBlockBuilder()
        builder.block()
        include(builder.build())
    }

    fun vertex(base: ShaderBlock? = null, block: VertexShaderBlockBuilder.() -> Unit) {
        if (base != null && base.type != ShaderBlockType.VERTEX) {
            error("Vertex base must be a vertex block!")
        }
        val builder = VertexShaderBlockBuilder(base ?: vertexBase)
        builder.block()
        val vertexIdx = blocks.indexOfFirst { it.type == ShaderBlockType.VERTEX }
        if (vertexIdx != -1) {
            blocks.removeAt(vertexIdx)
            blocks.add(vertexIdx, builder.build())
        } else {
            blocks.add(builder.build())
        }
    }

    fun fragment(base: ShaderBlock? = null, block: FragmentShaderBlockBuilder.() -> Unit) {
        if (base != null && base.type != ShaderBlockType.FRAGMENT) {
            error("Fragment base must be a fragment block!")
        }
        val builder = FragmentShaderBlockBuilder(base ?: fragmentBase)
        builder.block()
        val fragmentIdx = blocks.indexOfFirst { it.type == ShaderBlockType.FRAGMENT }
        if (fragmentIdx != -1) {
            blocks.removeAt(fragmentIdx)
            blocks.add(fragmentIdx, builder.build())
        } else {
            blocks.add(builder.build())
        }
    }

    fun compute(base: ShaderBlock? = null, block: ComputeShaderBlockBuilder.() -> Unit) {
        if (base != null && base.type != ShaderBlockType.COMPUTE) {
            error("Compute base must be a compute block!")
        }
        val builder = ComputeShaderBlockBuilder(base ?: computeBase)
        builder.block()
        val computeIdx = blocks.indexOfFirst { it.type == ShaderBlockType.COMPUTE }
        if (computeIdx != -1) {
            blocks.removeAt(computeIdx)
            blocks.add(computeIdx, builder.build())
        } else {
            blocks.add(builder.build())
        }
    }

    fun build(): ShaderCode {
        return ShaderCode(includes, blocks)
    }
}
