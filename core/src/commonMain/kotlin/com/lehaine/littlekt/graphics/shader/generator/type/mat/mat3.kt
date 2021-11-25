package com.lehaine.littlekt.graphics.shader.generator.type.mat

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.str
import com.lehaine.littlekt.graphics.shader.generator.type.Matrix
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Mat3(override val builder: GlslGenerator) : Matrix {

    override val typeName: String = "mat3"
    override var value: String? = null

    private var column1 by MatrixColumnDelegate(0, ::Vec3)
    private var column2 by MatrixColumnDelegate(1, ::Vec3)

    private var column3 by MatrixColumnDelegate(2, ::Vec3)

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun Mat3.get(i: Int): Vec3 {
        return when (i) {
            0 -> column1
            1 -> column2
            2 -> column3
            else -> throw Error("Column index $i out of range [0..2]")
        }
    }

    operator fun times(a: Float) = Mat3(builder, "(${this.value} * ${a.str()})")
    operator fun div(a: Float) = Mat3(builder, "(${this.value} / ${a.str()})")

    operator fun times(a: Vec3) = Vec3(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Vec3) = Vec3(builder, "(${this.value} / ${a.value})")

    operator fun times(a: Mat3) = Mat3(builder, "(${this.value} * ${a.value})")
    operator fun div(a: Mat3) = Mat3(builder, "(${this.value} / ${a.value})")
    operator fun plus(a: Mat3) = Mat3(builder, "(${this.value} + ${a.value})")
    operator fun minus(a: Mat3) = Mat3(builder, "(${this.value} - ${a.value})")

    operator fun unaryMinus() = Mat3(builder, "-(${this.value})")
}

operator fun Float.times(a: Mat3) = Mat3(a.builder, "(${this.str()} * ${a.value})")
operator fun Float.div(a: Mat3) = Mat3(a.builder, "(${this.str()} / ${a.value})")