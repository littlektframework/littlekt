package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
abstract class FragmentShader(
    private val shader: String
) {
    open val parameters: List<ShaderParameter> = emptyList()

    override fun toString(): String = shader
}