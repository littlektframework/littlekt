package com.lehaine.littlekt.graphics.shader.generator.type.vec

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.delegate.ComponentDelegate
import com.lehaine.littlekt.graphics.shader.generator.str
import com.lehaine.littlekt.graphics.shader.generator.type.Vector
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.floatComponent

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec2(override val builder: GlslGenerator) : Vector {
    override val typeName: String = "vec2"
    override var value: String? = null

    var x by floatComponent()
    var y by floatComponent()

    val xy: Vec2 by lazy { Vec2(builder, "${this.value}.xy") }
    val yx: Vec2 by lazy { Vec2(builder, "${this.value}.yx") }

    constructor(builder: GlslGenerator, value: String) : this(builder) {
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
    operator fun minus(a: Float) = Vec2(builder, "(${this.value} - ${a.str()})")

    operator fun unaryMinus() = Vec2(builder, "-(${this.value})")

    operator fun get(i: Int): GLFloat {
        return when (i) {
            0 -> x
            1 -> y
            else -> throw Error("Index $i out of range [0..1]")
        }
    }
}

operator fun Float.times(a: Vec2) = Vec2(a.builder, "(${this.str()} * ${a.value})")
operator fun Float.div(a: Vec2) = Vec2(a.builder, "(${this.str()} / ${a.value})")

fun vec2Component() = ComponentDelegate(::Vec2)