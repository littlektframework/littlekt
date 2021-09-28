package com.lehaine.littlekt.shader.vertex

import com.lehaine.littlekt.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
abstract class VertexShader(
    private val shader: String
) {
    open val parameters: List<ShaderParameter> = emptyList()

    override fun toString(): String = shader
}