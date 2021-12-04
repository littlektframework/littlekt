package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/28/2021
 */
class GlyphFragmentShader() : FragmentShaderModel() {
    val uColor get() = parameters[0] as ShaderParameter.UniformVec4

    private val u_color by uniform(::Vec4)
    private val v_color by varying(::Vec4)
    private val v_texCoords by varying(::Vec2)

    init {
        If(v_texCoords.x * v_texCoords.x - v_texCoords.y gt 0f) {
            discard()
        }

        var frontFaces by float("1.0 / 255.0")
        If(gl_FrontFacing) {
            frontFaces = ("16.0 / 255.0").float
        }
        gl_FragColor = u_color + v_color * frontFaces
    }
}