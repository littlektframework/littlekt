package com.lehaine.littlekt.shader.vertex

import com.lehaine.littlekt.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
//language=GLSL
private val simpleVertexShader =
    """
        #ifndef GL_ES
        #define lowp
        #define mediump
        #define highp
        #endif
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        attribute vec4 a_mix_color;
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        varying vec4 v_mix_color;
        varying vec2 v_texCoords;
        
        void main() {
            v_color = a_color;
            v_color.a = v_color.a * (255.0/254.0);
            v_mix_color = a_mix_color;
            v_mix_color.a *= (255.0/254.0);
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
    """.trimIndent()

class TexturedQuadShader : VertexShader(simpleVertexShader) {

    val aPosition = ShaderParameter.AttributeVec4("a_position")
    val aColor = ShaderParameter.AttributeVec4("a_color")
    val aTexCoord = ShaderParameter.AttributeVec2("a_texCoord0")
    val aMixColor = ShaderParameter.AttributeVec4("a_mix_color")
    val uProjTrans = ShaderParameter.UniformMat4("u_projTrans")

    override val parameters: List<ShaderParameter> = listOf(aPosition, aColor, aTexCoord, aMixColor, uProjTrans)
}