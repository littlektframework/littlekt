package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.GeneratedShader
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

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

class DefaultFragmentShader : GeneratedShader(), FragmentShader {

    private val v_color by varying(::Vec4)
    private val v_texCoords by varying(::Vec2)
    private val u_texture by uniform(::Sampler2D)

    init {
        gl_FragColor = v_color * texture2D(u_texture, v_texCoords)
    }
}