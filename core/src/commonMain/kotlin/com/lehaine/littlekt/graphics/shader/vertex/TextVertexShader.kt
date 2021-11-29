package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class TextVertexShader : VertexShaderModel() {
    private val rect by uniform(::Vec4)
    private val position by attribute(::Vec2)
    private var coord by varying(::Vec2)

    init {
        coord = mix(rect.xy, rect.zw, position * 0.5f + 0.5f)
        gl_Position = vec4Lit(coord * 2f - 1f, 0f, 1f)
    }
}