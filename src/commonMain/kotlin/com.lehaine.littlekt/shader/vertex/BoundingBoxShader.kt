package com.lehaine.littlekt.shader.vertex

import com.lehaine.littlekt.shader.ShaderParameter
import com.lehaine.littlekt.shader.ShaderParameter.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
//language=GLSL
private val simpleVertexShader =
    """
        #ifdef GL_ES
        precision highp float;
        #endif
        
        uniform mat4 uModelView;
        uniform vec4 uColor;
        
        attribute vec3 aVertexPosition;
        
        varying vec4 vColor;
        
        void main() {
            gl_Position = uModelView * vec4(aVertexPosition, 1.0);
            vColor = uColor;
        }
    """.trimIndent()

class BoundingBoxVertexShader : VertexShader(simpleVertexShader) {

    val uModelView =
        UniformMat4("uModelView")
    val uColor = UniformFloat("uColor")

    val aVertexPosition =
        AttributeVec3("aVertexPosition")

    override val parameters: List<ShaderParameter> = listOf(
        uModelView,
        uColor,
        aVertexPosition
    )
}