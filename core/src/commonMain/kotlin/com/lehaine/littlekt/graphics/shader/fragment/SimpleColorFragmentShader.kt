package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.GeneratedShader
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class SimpleColorFragmentShader : GeneratedShader(), FragmentShader {
    private val v_color by varying(::Vec4, Precision.LOW)

    init {
        gl_FragColor = v_color
    }
}