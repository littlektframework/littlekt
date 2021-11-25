package com.lehaine.littlekt.graphics.shader.generator.type.sampler

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Sampler2DArray(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "sampler2D"
    override var value: String? = null

    operator fun get(i: GLInt): Sampler2D {
        val result = Sampler2D(builder)
        result.value = "$value[${i.value}]"
        return result
    }
}