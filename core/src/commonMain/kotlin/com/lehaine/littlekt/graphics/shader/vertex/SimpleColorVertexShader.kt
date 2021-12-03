package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */

class SimpleColorVertexShader : VertexShaderModel() {
    private val u_projTrans by uniform(::Mat4)
    private val a_position by attribute(::Vec4)
    private val a_color by attribute(::Vec4)
    private var v_color by varying(::Vec4)

    init {
        v_color = a_color
        gl_Position = u_projTrans * a_position
    }
}