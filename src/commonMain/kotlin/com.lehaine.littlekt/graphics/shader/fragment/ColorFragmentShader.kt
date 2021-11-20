package com.lehaine.littlekt.graphics.shader.fragment

/**
 * @author Colton Daily
 * @date 9/28/2021
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
        
        varying vec4 vColor;
        void main() {
              gl_FragColor = vec4(1.0,1.0,1.0,1.0);
        }
    """.trimIndent()

class ColorFragmentShader : FragmentShader(simpleFragmentShader)