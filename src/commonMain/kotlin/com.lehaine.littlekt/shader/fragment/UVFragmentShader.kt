package com.lehaine.littlekt.shader.fragment

import com.lehaine.littlekt.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
//language=GLSL
private val simpleFragmentShader =
    """
        #ifdef GL_ES
        precision highp float;
        #endif
        
        varying vec2 vUVPosition;
        varying vec4 vLighting;
        uniform sampler2D uUV;
        void main() {
              vec4 texel = texture2D(uUV, vUVPosition);
              // If the light alpha is 0, the light will be vec3(1.0) so the texel will not be altered
              // otherwise, the light will be vLighting.rgb
              vec3 light = (vec3(1.0) * (vec3(1.0) - vec3(vLighting.a))) + vLighting.rgb * vLighting.a;
              gl_FragColor = vec4(texel.rgb * light, texel.a);
        }
    """.trimIndent()

class UVFragmentShader : FragmentShader(simpleFragmentShader) {

    val uUV =
        ShaderParameter.UniformSample2D("uUV")

    override val parameters: List<ShaderParameter> = listOf(uUV)
}