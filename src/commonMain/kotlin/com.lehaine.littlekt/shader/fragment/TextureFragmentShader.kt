package com.lehaine.littlekt.shader.fragment

import com.lehaine.littlekt.shader.ShaderParameter

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
//language=GLSL
private val simpleFragmentShader =
    """
        #ifdef GL_ES
        precision highp float;
        precision mediump int;
        #else
        #define lowp
        #define mediump
        #define highp
        #endif
        
        varying lowp vec4 v_color;
        varying lowp vec4 v_mix_color;
        varying highp vec2 v_texCoords;
        uniform highp sampler2D u_texture;
        
        void main() {
            vec4 c = texture2D(u_texture, v_texCoords);
            gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);
        }
    """.trimIndent()

class TextureFragmentShader : FragmentShader(simpleFragmentShader) {
    val uTexture = ShaderParameter.UniformSample2D("u_texture")

    override val parameters: List<ShaderParameter> = listOf(uTexture)
}