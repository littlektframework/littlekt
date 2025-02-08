package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
data class ShaderStructEntry(
    val offset: Int,
    val size: Int,
    val alignment: Int,
    val type: ShaderStructParameterType,
)
