package com.lehaine.littlekt.graphics.shader.generator.type.scalar

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.GenType

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class GLInt(override val builder: GlslGenerator) : GenType {
    override val typeName: String = "int"
    override var value: String? = null

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun plus(a: GLInt) = GLInt(builder, "(${this.value} + ${a.value})")
    operator fun plus(a: Int) = GLInt(builder, "(${this.value} + $a)")

    val float get() =  GLFloat(builder, "float(${this.value})")
}