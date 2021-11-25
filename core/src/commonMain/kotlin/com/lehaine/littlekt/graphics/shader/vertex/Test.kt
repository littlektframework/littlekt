package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.GeneratedShader
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

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

class TestShader : GeneratedShader() {
    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)
    val u_projTrans by uniform(::Mat4)
    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)

    init {
        v_color = a_color
        val alpha by float(255f / 254f)
        v_color.w = v_color.w * alpha
        v_texCoords = a_texCoord0
        gl_Position = u_projTrans * a_position
    }

}