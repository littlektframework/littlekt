package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ComputeShaderBlockBuilder(base: ShaderBlock? = null) : ShaderBlockBuilder(base) {
    override var type = ShaderBlockType.COMPUTE

    fun main(
        workGroupSizeX: Int,
        workGroupSizeY: Int = workGroupSizeX,
        workGroupSizeZ: Int = workGroupSizeY,
        entry: String = "main",
        block: ShaderBlockBuilder.() -> String,
    ) {

        body =
            "%compute_start%\n@compute @workgroup_size(${workGroupSizeX}, ${workGroupSizeY}, ${workGroupSizeZ}) fn $entry(@builtin(global_invocation_id) global_id : vec3u) {\n${block()}\n}\n%compute_end%"
    }
}
