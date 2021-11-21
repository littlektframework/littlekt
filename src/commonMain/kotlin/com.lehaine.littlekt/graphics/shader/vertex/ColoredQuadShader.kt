package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
//language=GLSL
private val simpleVertexShader =
    """
        attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
        attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        
        void main() {
            v_color = a_color;
            gl_Position = u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
        }
    """.trimIndent()

class ColoredQuadShader : VertexShader(simpleVertexShader) {

    val aPosition = ShaderParameter.AttributeVec4(ShaderProgram.POSITION_ATTRIBUTE)
    val aColor = ShaderParameter.AttributeVec4(ShaderProgram.COLOR_ATTRIBUTE)

    override val parameters: List<ShaderParameter> = listOf(aPosition, aColor, uProjTrans)
}