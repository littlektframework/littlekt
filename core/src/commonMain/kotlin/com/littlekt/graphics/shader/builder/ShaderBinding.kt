package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
data class ShaderBinding(
    val group: Int,
    val binding: Int,
    val varName: String,
    val type: ShaderStructParameterType,
)
