package com.lehaine.littlekt.graphics.shader.generator.type.vec

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.str
import com.lehaine.littlekt.graphics.shader.generator.type.Vector
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.floatComponent

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec4(override val builder: GlslGenerator) : Vector {
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

    val xxxx: Vec4 by lazy { Vec4(builder, "${this.value}.xyzw") }
    val xxxw: Vec4 by lazy { Vec4(builder, "${this.value}.xxxw") }
    val xxyw: Vec4 by lazy { Vec4(builder, "${this.value}.xxyw") }
    val xxzw: Vec4 by lazy { Vec4(builder, "${this.value}.xxzw") }
    val xyxw: Vec4 by lazy { Vec4(builder, "${this.value}.xyxw") }
    val xyyw: Vec4 by lazy { Vec4(builder, "${this.value}.xyyw") }
    val xyzw: Vec4 by lazy { Vec4(builder, "${this.value}.xyzw") }
    val xzxw: Vec4 by lazy { Vec4(builder, "${this.value}.xzxw") }
    val xzyw: Vec4 by lazy { Vec4(builder, "${this.value}.xzyw") }
    val xzzw: Vec4 by lazy { Vec4(builder, "${this.value}.xzzw") }
    val xywz: Vec4 by lazy { Vec4(builder, "${this.value}.xywz") }
    val xwyz: Vec4 by lazy { Vec4(builder, "${this.value}.xwyz") }

    val yxxw: Vec4 by lazy { Vec4(builder, "${this.value}.yxxw") }
    val yxyw: Vec4 by lazy { Vec4(builder, "${this.value}.yxyw") }
    val yxzw: Vec4 by lazy { Vec4(builder, "${this.value}.yxzw") }
    val yyxw: Vec4 by lazy { Vec4(builder, "${this.value}.yyxw") }
    val yyyw: Vec4 by lazy { Vec4(builder, "${this.value}.yyyw") }
    val yyzw: Vec4 by lazy { Vec4(builder, "${this.value}.yyzw") }
    val yzxw: Vec4 by lazy { Vec4(builder, "${this.value}.yzxw") }
    val yzyw: Vec4 by lazy { Vec4(builder, "${this.value}.yzyw") }
    val yzzw: Vec4 by lazy { Vec4(builder, "${this.value}.yzzw") }
    val yzww: Vec4 by lazy { Vec4(builder, "${this.value}.yzww") }
    val ywxz: Vec4 by lazy { Vec4(builder, "${this.value}.ywxz") }
    val ywzz: Vec4 by lazy { Vec4(builder, "${this.value}.ywzz") }
    val yxwz: Vec4 by lazy { Vec4(builder, "${this.value}.yxwz") }
    val yxwx: Vec4 by lazy { Vec4(builder, "${this.value}.yxwx") }
    val ywww: Vec4 by lazy { Vec4(builder, "${this.value}.ywww") }
    val yxww: Vec4 by lazy { Vec4(builder, "${this.value}.yxww") }

    val zxxw: Vec4 by lazy { Vec4(builder, "${this.value}.zxxw") }
    val zxyw: Vec4 by lazy { Vec4(builder, "${this.value}.zxyw") }
    val zxzw: Vec4 by lazy { Vec4(builder, "${this.value}.zxzw") }
    val zyxw: Vec4 by lazy { Vec4(builder, "${this.value}.zyxw") }
    val zyyw: Vec4 by lazy { Vec4(builder, "${this.value}.zyyw") }
    val zyzw: Vec4 by lazy { Vec4(builder, "${this.value}.zyzw") }
    val zzxw: Vec4 by lazy { Vec4(builder, "${this.value}.zzxw") }
    val zzyw: Vec4 by lazy { Vec4(builder, "${this.value}.zzyw") }
    val zzzw: Vec4 by lazy { Vec4(builder, "${this.value}.zzzw") }

    val wxyz: Vec4 by lazy { Vec4(builder, "${this.value}.wxyz") }
    val wzyx: Vec4 by lazy { Vec4(builder, "${this.value}.wzyx") }
    val wwww: Vec4 by lazy { Vec4(builder, "${this.value}.wwww") }
    val wxxx: Vec4 by lazy { Vec4(builder, "${this.value}.wxxx") }
    val wxxy: Vec4 by lazy { Vec4(builder, "${this.value}.wxxy") }
    val wxxw: Vec4 by lazy { Vec4(builder, "${this.value}.wxxw") }
    val wxyy: Vec4 by lazy { Vec4(builder, "${this.value}.wxyy") }
    val wyyy: Vec4 by lazy { Vec4(builder, "${this.value}.wyyy") }
    val wxwy: Vec4 by lazy { Vec4(builder, "${this.value}.wxwy") }

    constructor(builder: GlslGenerator, value: String) : this(builder) {
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