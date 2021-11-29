package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface Shader {
    var source: String
    val parameters: List<ShaderParameter>
}

interface FragmentShader : Shader
interface VertexShader : Shader

open class FragmentShaderModel : GlslGenerator(), FragmentShader {
    override var source: String = ""
        get() {
            if (field.isBlank()) {
                field = generate()
            }
            return field
        }
}

open class VertexShaderModel : GlslGenerator(), VertexShader {
    override var source: String = ""
        get() {
            if (field.isBlank()) {
                field = generate()
            }
            return field
        }
}