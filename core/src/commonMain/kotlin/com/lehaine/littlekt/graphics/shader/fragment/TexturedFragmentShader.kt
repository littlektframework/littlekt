package com.lehaine.littlekt.graphics.shader.fragment

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
//language=GLSL
private val simpleFragmentShader =
    """
        #ifdef GL_ES
        #define LOWP lowp
        precision mediump float;
        #else
        #define LOWP 
        #endif
        
        varying LOWP vec4 v_color;
        varying vec2 v_texCoords;
        uniform sampler2D u_texture;
        
        void main() {
            gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
        }
    """.trimIndent()

class TexturedFragmentShader : FragmentShader(simpleFragmentShader) {

}