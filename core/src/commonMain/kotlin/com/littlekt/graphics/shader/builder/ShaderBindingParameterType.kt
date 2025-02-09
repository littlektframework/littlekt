package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
sealed class ShaderBindingParameterType(val name: String) {

    data class Struct(val struct: ShaderStruct) : ShaderBindingParameterType(struct.name)

    sealed class WgslType(name: String) : ShaderBindingParameterType(name) {
        data object texture_f32 : WgslType("texture_2d<f32>")

        data object sampler : WgslType("sampler")
    }

    data class Array(val type: ShaderBindingParameterType, val length: Int) :
        ShaderBindingParameterType("array<${type.name}, $length>")
}
