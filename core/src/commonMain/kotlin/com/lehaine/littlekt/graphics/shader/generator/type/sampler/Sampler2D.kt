package com.lehaine.littlekt.graphics.shader.generator.type.sampler

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Sampler2D(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "sampler2D"
    override var value: String? = null
}

class ShadowTexture2D(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "sampler2D"
    override var value: String? = null
}