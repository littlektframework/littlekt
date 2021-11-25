package com.lehaine.littlekt.graphics.shader.builder.type.sampler

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.Variable

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Sampler2D(override val builder: ShaderBuilder) : Variable {
    override val typeName: String = "sampler2D"
    override var value: String? = null
}

class ShadowTexture2D(override val builder: ShaderBuilder) : Variable {
    override val typeName: String = "sampler2D"
    override var value: String? = null
}