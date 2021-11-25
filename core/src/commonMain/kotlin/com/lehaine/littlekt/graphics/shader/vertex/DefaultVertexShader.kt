package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.GeneratedShader
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShader
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class DefaultVertexShader : GeneratedShader(), VertexShader {
    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)
    private val u_projTrans by uniform(::Mat4)
    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)

    init {
        v_color = a_color
        val alpha by float(255f / 254f)
        v_color.w = v_color.w * alpha
        v_texCoords = a_texCoord0
        gl_Position = u_projTrans * a_position
    }

}