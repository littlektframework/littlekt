package com.lehaine.littlekt.graphics.shader.builder.type.scalar

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.type.GenType

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class GLInt(override val builder: ShaderBuilder) : GenType {
    override val typeName: String = "int"
    override var value: String? = null

    constructor(builder: ShaderBuilder, value: String) : this(builder) {
        this.value = value
    }

    operator fun plus(a: GLInt) = GLInt(builder, "(${this.value} + ${a.value})")
    operator fun plus(a: Int) = GLInt(builder, "(${this.value} + $a)")
}