package com.lehaine.littlekt.graphics.shader.builder.type.vec

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.str
import com.lehaine.littlekt.graphics.shader.builder.type.Vector
import com.lehaine.littlekt.graphics.shader.builder.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.builder.type.scalar.floatComponent

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec4(override val builder: ShaderBuilder) : Vector {
    override val typeName: String = "vec4"
    override var value: String? = null

    var x by floatComponent()
    var y by floatComponent()
    var z by floatComponent()
    var w by floatComponent()

    var xx by vec2Component()
    var xy by vec2Component()
    var xz by vec2Component()
    var xw by vec2Component()
    var yx by vec2Component()
    var yy by vec2Component()
    var yz by vec2Component()
    var yw by vec2Component()
    var zx by vec2Component()
    var zy by vec2Component()
    var zz by vec2Component()
    var zw by vec2Component()
    var wx by vec2Component()
    var wy by vec2Component()
    var wz by vec2Component()
    var ww by vec2Component()

    var xxx by vec3Component()
    var xxy by vec3Component()
    var xxz by vec3Component()
    var xyx by vec3Component()
    var xyy by vec3Component()
    var xyz by vec3Component()
    var xzx by vec3Component()
    var xzy by vec3Component()
    var xzz by vec3Component()

    var yxx by vec3Component()
    var yxy by vec3Component()
    var yxz by vec3Component()
    var yyx by vec3Component()
    var yyy by vec3Component()
    var yyz by vec3Component()
    var yzx by vec3Component()
    var yzy by vec3Component()
    var yzz by vec3Component()
    var yzw by vec3Component()

    var zxx by vec3Component()
    var zxy by vec3Component()
    var zxz by vec3Component()
    var zyx by vec3Component()
    var zyy by vec3Component()
    var zyz by vec3Component()
    var zzx by vec3Component()
    var zzy by vec3Component()
    var zzz by vec3Component()

    var www by vec3Component()

    constructor(builder: ShaderBuilder, value: String) : this(builder) {
        this.value = value
    }

    operator fun times(a: GLFloat) = Vec4(builder, "(${this.value} * ${a.value})")
    operator fun div(a: GLFloat) = Vec4(builder, "(${this.value} / ${a.value})")

    operator fun times(a: Float) = Vec4(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = Vec4(builder, "(${this.value} / ${a.str()})")

    operator fun times(a: Vec4) = Vec4(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Vec4) = Vec4(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: Vec4) = Vec4(builder, "(${this.value} + ${a.value})")
    operator fun minus(a: Vec4) = Vec4(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = Vec4(builder, "-(${this.value})")
}

operator fun Float.times(a: Vec4) = Vec4(a.builder, "(${this.str()} * ${a.value})")
operator fun Float.div(a: Vec4) = Vec4(a.builder, "(${this.str()} / ${a.value})")
operator fun GLFloat.times(a: Vec4) = Vec4(a.builder, "(${this.value} * ${a.value})")