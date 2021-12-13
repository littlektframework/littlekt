package com.lehaine.littlekt.graphics.shader.generator.type

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Bool(override val builder: GlslGenerator) : GenType {
    override val typeName: String = "bool"
    override var value: String? = null

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    infix fun or(a: Bool): Bool = Bool(builder, "(${this.value} || ${a.value})")
    infix fun and(a: Bool): Bool = Bool(builder, "(${this.value} && ${a.value})")
}