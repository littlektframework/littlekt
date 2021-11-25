package com.lehaine.littlekt.graphics.shader.builder.type.mat

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder
import com.lehaine.littlekt.graphics.shader.builder.str
import com.lehaine.littlekt.graphics.shader.builder.type.Matrix
import com.lehaine.littlekt.graphics.shader.builder.type.vec.Vec2

/**
 * @author Colton Daily
 * @date 11/25/2021
 */

class Mat2(override val builder: ShaderBuilder) : Matrix {

    override val typeName: String = "mat2"
    override var value: String? = null

    private var column1 by MatrixColumnDelegate(0, ::Vec2)
    private var column2 by MatrixColumnDelegate(1, ::Vec2)

    constructor(builder: ShaderBuilder, value: String) : this(builder) {
        this.value = value
    }

    operator fun get(i: Int): Vec2 {
        return when (i) {
            0 -> column1
            1 -> column2
            else -> throw Error("Column index $i out of range [0..1]")
        }
    }

    operator fun times(a: Float) = Mat2(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = Mat2(builder, "(${this.value} / ${a.str()})")

    operator fun times(a: Vec2) = Vec2(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Vec2) = Vec2(builder, "(${this.value} / ${a.value})")

    operator fun times(a: Mat2) = Mat2(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Mat2) = Mat2(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: Mat2) = Mat2(builder, "(${this.value} + ${a.value})")
    operator fun minus(a: Mat2) = Mat2(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = Mat2(builder, "-(${this.value})")
}

operator fun Float.times(a: Mat2) = Mat2(a.builder, "(${this.str()}} * ${a.value})")
operator fun Float.div(a: Mat2) = Mat2(a.builder, "(${this.str()}} / ${a.value})")