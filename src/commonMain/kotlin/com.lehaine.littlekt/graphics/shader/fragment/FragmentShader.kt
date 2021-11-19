package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
abstract class FragmentShader(
    private val shader: String
) {
    val uTexture = ShaderParameter.UniformSample2D("u_texture")
    open val parameters: List<ShaderParameter> = listOf(uTexture)

    override fun toString(): String = shader
}