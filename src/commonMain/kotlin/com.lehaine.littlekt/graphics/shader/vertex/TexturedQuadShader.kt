package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
//language=GLSL
private val simpleVertexShader =
    """
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        varying vec2 v_texCoords;
        
        void main() {
            v_color = a_color;
            v_color.a = v_color.a * (255.0/254.0);
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
    """.trimIndent()

class TexturedQuadShader : VertexShader(simpleVertexShader) {

    val aPosition = ShaderParameter.AttributeVec4("a_position")
    val aColor = ShaderParameter.AttributeVec4("a_color")
    val aTexCoord = ShaderParameter.AttributeVec2("a_texCoord0")

    override val parameters: List<ShaderParameter> = listOf(aPosition, aColor, aTexCoord, uProjTrans)
}