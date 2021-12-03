package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class GlyphVertexShader : VertexShaderModel() {
    val uProjTrans get() = parameters[0] as ShaderParameter.UniformMat4

    private val u_projTrans by uniform(::Mat4)
    private val a_position by attribute(::Vec2)
    private val a_color by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)
    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)

    init {
        v_texCoords = a_texCoord0
        v_color = a_color
        gl_Position = u_projTrans * vec4Lit(a_position, 0f, 1f)
    }
}