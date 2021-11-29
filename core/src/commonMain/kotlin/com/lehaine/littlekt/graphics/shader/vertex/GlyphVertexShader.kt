package com.lehaine.littlekt.graphics.shader.vertex

import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class GlyphVertexShader : VertexShaderModel() {
    private val view by uniform(::Mat3)
    private var coord2 by varying(::Vec2)
    private val position by attribute(::Vec4)


    init {
        coord2 = position.zw
        gl_Position = vec4Lit(view * vec3Lit(position.xy, 1f), 0f).xywz
    }
}