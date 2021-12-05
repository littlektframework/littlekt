package com.lehaine.littlekt.graphics.shader.fragment

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/28/2021
 */
class GlyphFragmentShader : FragmentShaderModel() {
    private val v_color by varying(::Vec4)
    private val v_texCoords by varying(::Vec2)

    init {
        If(v_texCoords.x * v_texCoords.x - v_texCoords.y gt 0f) {
            discard()
        }

        gl_FragColor = v_color
    }
}