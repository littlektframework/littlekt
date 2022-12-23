package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class DefaultVertexShader : VertexShaderModel() {
    val uProjTrans get() = parameters[0] as ShaderParameter.UniformMat4

    private val u_projTrans by uniform(::Mat4)

    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)

    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)

    init {
        v_color = a_color
        val alpha by float { 255f.lit / 254f.lit }
        v_color.w = v_color.w * alpha
        v_texCoords = a_texCoord0
        gl_Position = u_projTrans * a_position
    }

}

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