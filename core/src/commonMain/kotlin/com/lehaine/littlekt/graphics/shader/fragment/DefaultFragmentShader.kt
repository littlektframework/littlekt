package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class DefaultFragmentShader : FragmentShaderModel() {
    val uTexture get() = parameters[0] as ShaderParameter.UniformSample2D

    private val u_texture by uniform(::Sampler2D)
    private val v_color by varying(::Vec4, Precision.LOW)
    private val v_texCoords by varying(::Vec2)

    init {
        gl_FragColor = v_color * texture2D(u_texture, v_texCoords)
    }
}