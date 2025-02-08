package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderStruct(val name: String, parameters: Map<String, ShaderStructParameterType>) {
    val layout: Map<String, ShaderStructEntry>
    val size: Int
    val alignment: Int

    init {
        var currentOffset = 0
        var maxAlignment = 0

        layout =
            parameters
                .map { (name, type) ->
                    val alignment = type.alignment()
                    val alignedOffset = align(currentOffset, alignment)

                    val entry = ShaderStructEntry(alignedOffset, type.size(), alignment, type)
                    currentOffset = alignedOffset + entry.size
                    maxAlignment = maxOf(maxAlignment, alignment)

                    name to entry
                }
                .toMap()

        alignment = maxAlignment
        size = align(currentOffset, alignment)
    }

    private fun align(offset: Int, alignment: Int): Int {
        return (offset + alignment - 1) / alignment * alignment
    }

    override fun toString(): String {
        return "ShaderStruct(name='$name', layout=$layout, size=$size, alignment=$alignment)"
    }
}
