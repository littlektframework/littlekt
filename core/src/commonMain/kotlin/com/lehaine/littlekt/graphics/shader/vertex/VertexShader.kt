package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
abstract class VertexShader(
    private val shader: String
) {
    val uProjTrans = ShaderParameter.UniformMat4("u_projTrans")
    open val parameters: List<ShaderParameter> = listOf(uProjTrans)

    override fun toString(): String = shader
}