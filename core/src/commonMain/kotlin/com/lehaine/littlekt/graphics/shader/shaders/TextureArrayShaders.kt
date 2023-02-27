package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2DArray
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
class TextureArrayVertexShader : VertexShaderModel() {
    val uProjTrans get() = parameters["u_projTrans"] as ShaderParameter.UniformMat4

    private val u_projTrans by uniform(::Mat4)

    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)
    private val a_textureIndex by attribute(::GLFloat)

    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)
    private var v_textureIndex by varying(::GLFloat)

    init {
        v_color = a_color
        val alpha by float { 255f.lit / 254f.lit }
        v_color.w = v_color.w * alpha
        v_texCoords = a_texCoord0
        v_textureIndex = a_textureIndex
        gl_Position = u_projTrans * a_position
    }

}

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
class TextureArrayFragmentShader : FragmentShaderModel() {
    private val u_textureArray by uniform(::Sampler2DArray, Precision.HIGH)

    private val v_textureIndex by varying(::GLFloat)
    private val v_color by varying(::Vec4, Precision.LOW)
    private val v_texCoords by varying(::Vec2)

    init {
        gl_FragColor = v_color * texture(u_textureArray, vec3(v_texCoords, v_textureIndex).lit).lit
    }
}