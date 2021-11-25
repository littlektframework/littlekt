package com.lehaine.littlekt.graphics.shader.builder.type.vec

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.delegate.ComponentDelegate
import com.lehaine.littlekt.graphics.shader.builder.str
import com.lehaine.littlekt.graphics.shader.builder.type.Vector
import com.lehaine.littlekt.graphics.shader.builder.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.builder.type.scalar.floatComponent

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec2(override val builder: ShaderBuilder) : Vector {
    override val typeName: String = "vec2"
    override var value: String? = null

    var x by floatComponent()
    var y by floatComponent()

    constructor(builder: ShaderBuilder, value: String) : this(builder) {
        this.value = value
    }

    operator fun times(a: Float) = Vec2(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = Vec2(builder, "(${this.value} / ${a.str()})")

    operator fun times(a: GLFloat) = Vec2(builder, "(${this.value} * ${a.value})")
    operator fun div(a: GLFloat) = Vec2(builder, "(${this.value} / ${a.value})")

    operator fun times(a: Vec2) = Vec2(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Vec2) = Vec2(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: Vec2) = Vec2(builder, "(${this.value} + ${a.value})")
    operator fun plus(a: Float) = Vec2(builder, "(${this.value} + ${a.str()})")
    operator fun minus(a: Vec2) = Vec2(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = Vec2(builder, "-(${this.value})")
}

operator fun Float.times(a: Vec2) = Vec2(a.builder, "(${this.str()} * ${a.value})")
operator fun Float.div(a: Vec2) = Vec2(a.builder, "(${this.str()} / ${a.value})")

fun vec2Component() = ComponentDelegate(::Vec2)