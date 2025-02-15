package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
sealed class ShaderBindingParameterType(val name: String) {

    data class Struct(val struct: ShaderStruct) : ShaderBindingParameterType(struct.name)

    sealed class WgslType(name: String) : ShaderBindingParameterType(name) {
        data object texture_f32 : WgslType("texture_2d<f32>")

        data object texture_i32 : WgslType("texture_2d<i32>")

        data object texture_u32 : WgslType("texture_2d<u32>")

        data object texture_depth_2d : WgslType("texture_depth_2d")

        data object sampler : WgslType("sampler")

        data object sampler_comparison : WgslType("sampler_comparison")
    }

    data class Array(val type: ShaderBindingParameterType, val length: Int) :
        ShaderBindingParameterType(
            if (length > 0) "array<${type.name}, $length>" else "array<${type.name}>"
        ) {
        constructor(type: ShaderBindingParameterType) : this(type, -1)
    }
}
