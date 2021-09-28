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
        // Light information
        uniform vec3 uLightPosition;
        uniform vec4 uLightColor;
        
        attribute vec3 aVertexPosition;
        attribute vec3 aVertexNormal;
        attribute vec2 aUVPosition;
        
        varying vec2 vUVPosition;
        varying vec4 vLighting;
        
        void main() {
            vec3 vPosToLight = normalize(aVertexPosition - uLightPosition);
        	float directional = max(0.0, dot(aVertexNormal, -vPosToLight));
            gl_Position = uModelView * vec4(aVertexPosition, 1.0);
            vUVPosition = aUVPosition;
            
            vLighting = uLightColor + uLightColor * directional;
        }
    """.trimIndent()

class MeshVertexShader : VertexShader(
    shader = simpleVertexShader
) {
    val uModelView = UniformMat4("uModelView")
    val aVertexPosition = AttributeVec3("aVertexPosition")
    val aVertexNormal = AttributeVec3("aVertexNormal")
    val aUVPosition = AttributeVec2("aUVPosition")

    val uLightPosition = UniformVec3("uLightPosition")
    val uLightColor = UniformVec4("uLightColor")

    override val parameters: List<ShaderParameter> = listOf(
        uModelView,
        aVertexPosition,
        aVertexNormal,
        aUVPosition,
        uLightPosition,
        uLightColor
    )
}