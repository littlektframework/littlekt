package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ComputeShaderBlockBuilder(base: ComputeShaderBlock? = null) :
    MainShaderBlockBuilder<ComputeShaderBlock>(ComputeShaderBlock.BLOCK_NAME, base) {
    override var entry: String = "cmp_main"

    fun main(
        workGroupSizeX: Int,
        workGroupSizeY: Int = workGroupSizeX,
        workGroupSizeZ: Int = workGroupSizeY,
        entry: String = this.entry,
        block: ShaderBlockBuilder.() -> String,
    ) {
        this.entry = entry
        body =
            "%compute_start%\n@compute @workgroup_size(${workGroupSizeX}, ${workGroupSizeY}, ${workGroupSizeZ}) fn $entry(@builtin(global_invocation_id) global_id : vec3u) {\n${block()}\n}\n%compute_end%"
    }

    override fun build(extraStructs: Set<ShaderStruct>): ComputeShaderBlock {
        structs.addAll(extraStructs)
        return ComputeShaderBlock(entry, structs, bindingGroups, blocks, rules, body)
    }
}
