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
        attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        varying vec2 v_texCoords;
        
        void main() {
            v_color = a_color;
            v_color.a = v_color.a * (255.0/254.0);
            v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
            gl_Position = u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
        }
    """.trimIndent()

class TexturedQuadShader : VertexShader(simpleVertexShader) {

    val aPosition = ShaderParameter.AttributeVec4(ShaderProgram.POSITION_ATTRIBUTE)
    val aColor = ShaderParameter.AttributeVec4(ShaderProgram.COLOR_ATTRIBUTE)
    val aTexCoord = ShaderParameter.AttributeVec2("${ShaderProgram.TEXCOORD_ATTRIBUTE}0")

    override val parameters: List<ShaderParameter> = listOf(aPosition, aColor, aTexCoord, uProjTrans)
}