package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.GeneratedShader
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class DefaultFragmentShader : GeneratedShader(), FragmentShader {

    private val v_color by varying(::Vec4, Precision.LOW)
    private val v_texCoords by varying(::Vec2)
    private val u_texture by uniform(::Sampler2D)

    init {
        gl_FragColor = v_color * texture2D(u_texture, v_texCoords)
    }
}