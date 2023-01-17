package com.lehaine.littlekt.graphics.shader.generator.type.scalar

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.delegate.ComponentDelegate
import com.lehaine.littlekt.graphics.shader.generator.str
import com.lehaine.littlekt.graphics.shader.generator.type.Bool
import com.lehaine.littlekt.graphics.shader.generator.type.GenType
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class GLFloat(override val builder: GlslGenerator) : GenType {

    override val typeName: String = "float"
    override var value: String? = null

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun times(a: Float) = GLFloat(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = GLFloat(builder, "(${this.value} / ${a.str()})")
    operator fun plus(a: Float) = GLFloat(builder, "(${this.value} + ${a.str()})")
    operator fun minus(a: Float) = GLFloat(builder, "(${this.value} - ${a.str()})")

    operator fun times(a: GLFloat) = GLFloat(builder, "(${this.value} * ${a.value})")
    operator fun div(a: GLFloat) = GLFloat(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: GLFloat) = GLFloat(builder, "(${this.value} + ${a.value})")
    operator fun minus(a: GLFloat) = GLFloat(builder, "(${this.value} - ${a.value})")

    operator fun times(a: Vec2) = Vec2(a.builder, "(${this.value} * ${a.value})")
    operator fun times(a: Vec3) = Vec3(a.builder, "(${this.value} * ${a.value})")
    operator fun times(a: Vec4) = Vec4(a.builder, "(${this.value} * ${a.value})")

    operator fun minus(a: Vec2) = Vec2(builder, "(${this.value} - ${a.value})")
    operator fun minus(a: Vec3) = Vec3(builder, "(${this.value} - ${a.value})")
    operator fun minus(a: Vec4) = Vec4(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = GLFloat(builder, "-(${this.value})")

    infix fun setTo(a: Float) = GLFloat(builder, a.str())

    infix fun eq(a: GLFloat) = Bool(builder, "(${this.value} == ${a.value})")
    infix fun gte(a: GLFloat) = Bool(builder, "(${this.value} >= ${a.value})")
    infix fun gt(a: GLFloat) = Bool(builder, "(${this.value} > ${a.value})")
    infix fun lte(a: GLFloat) = Bool(builder, "(${this.value} <= ${a.value})")
    infix fun lt(a: GLFloat) = Bool(builder, "(${this.value} < ${a.value})")

    infix fun eq(a: Int) = Bool(builder, "(${this.value} == $a)")
    infix fun gte(a: Int) = Bool(builder, "(${this.value} >= $a)")


    infix fun eq(a: GLInt) = Bool(builder, "(${this.value} == ${a.value})")
    infix fun gte(a: GLInt) = Bool(builder, "(${this.value} >= ${a.value})")

    infix fun eq(a: Float) = Bool(builder, "(${this.value} == ${a.str()})")
    infix fun gte(a: Float) = Bool(builder, "(${this.value} >= ${a.str()})")
    infix fun gt(a: Float) = Bool(builder, "(${this.value} > ${a.str()})")
    infix fun lte(a: Float) = Bool(builder, "(${this.value} <= ${a.str()})")
    infix fun lt(a: Float) = Bool(builder, "(${this.value} < ${a.str()})")


    val int get() =  GLInt(builder, "int(${this.value})")
}

fun floatComponent() = ComponentDelegate(::GLFloat)