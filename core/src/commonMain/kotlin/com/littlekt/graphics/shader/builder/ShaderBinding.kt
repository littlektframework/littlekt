package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
data class ShaderBinding(
    val group: Int,
    val binding: Int,
    val varName: String,
    val paramType: ShaderBindingParameterType,
    val bindingType: ShaderBindingType,
) : ShaderSrc() {
    override val src: String by lazy {
        "@group($group) @binding($binding) var${bindingType.name} $varName: ${paramType.name};"
    }
}
