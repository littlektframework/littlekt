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
class Vec3(override val builder: GlslGenerator) : Vector {

    override val typeName: String = "vec3"
    override var value: String? = null

    var x by floatComponent()
    var y by floatComponent()
    var z by floatComponent()

    var xx by vec2Component()
    var xy by vec2Component()
    var xz by vec2Component()
    var yx by vec2Component()
    var yy by vec2Component()
    var yz by vec2Component()
    var zx by vec2Component()
    var zy by vec2Component()
    var zz by vec2Component()

    val xyz: Vec3 by lazy { Vec3(builder, "${this.value}.xyz") }
    val xzy: Vec3 by lazy { Vec3(builder, "${this.value}.xzy") }
    val yxz: Vec3 by lazy { Vec3(builder, "${this.value}.yxz") }
    val yzx: Vec3 by lazy { Vec3(builder, "${this.value}.yzx") }
    val zyx: Vec3 by lazy { Vec3(builder, "${this.value}.zyx") }
    val zxy: Vec3 by lazy { Vec3(builder, "${this.value}.zxy") }

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun times(a: Float) = Vec3(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = Vec3(builder, "(${this.value} / ${a.str()})")

    operator fun times(a: GLFloat) = Vec3(builder, "(${this.value} * ${a.value})")
    operator fun div(a: GLFloat) = Vec3(builder, "(${this.value} / ${a.value})")

    operator fun times(a: Vec3) = Vec3(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Vec3) = Vec3(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: Vec3) = Vec3(builder, "(${this.value} + ${a.value})")
    operator fun minus(a: Vec3) = Vec3(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = Vec3(builder, "-(${this.value})")
}

operator fun Float.times(a: Vec3) = Vec3(a.builder, "(${this.str()} * ${a.value})")
operator fun Float.div(a: Vec3) = Vec3(a.builder, "(${this.str()} / ${a.value})")
operator fun GLFloat.times(a: Vec3) = Vec3(a.builder, "(${this.value} * ${a.value})")

fun vec3Component() = ComponentDelegate(::Vec3)