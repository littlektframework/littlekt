package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class SimpleColor3DVertexShader : VertexShaderModel() {
    private val u_projTrans by uniform(::Mat4)
    private val a_position by attribute(::Vec4)
    private var v_color by varying(::Vec4)

    init {
        v_color = vec4Lit(1f, 1f, 1f, 1f)
        gl_Position = u_projTrans * a_position
    }
}

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class SimpleColor3DFragmentShader : FragmentShaderModel() {
    private val v_color by varying(::Vec4, Precision.LOW)

    init {
        gl_FragColor = v_color
    }
}
