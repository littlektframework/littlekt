package com.lehaine.littlekt.graphics.shader.generator.type.mat

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Matrix
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt

/**
 * @author Colton Daily
 * @date 1/16/2023
 */
class Mat4Array(override val builder: GlslGenerator) : Matrix {
    override val typeName: String = "mat4"
    override var value: String? = null

    operator fun get(i: GLInt): Mat4 {
        return Mat4(builder, "$value[${i.value}]")
    }
}