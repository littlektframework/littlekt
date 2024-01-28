package com.lehaine.littlekt.graphics.shader.generator.type.vec

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.ArrayItemDelegate
import com.lehaine.littlekt.graphics.shader.generator.type.Variable

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec2Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec2"
    override var value: String? = null

    private var column1 by ArrayItemDelegate(0, ::Vec2)
    private var column2 by ArrayItemDelegate(1, ::Vec2)
    private var column3 by ArrayItemDelegate(2, ::Vec2)
    private var column4 by ArrayItemDelegate(3, ::Vec2)
    private var column5 by ArrayItemDelegate(4, ::Vec2)
    private var column6 by ArrayItemDelegate(5, ::Vec2)
    private var column7 by ArrayItemDelegate(6, ::Vec2)
    private var column8 by ArrayItemDelegate(7, ::Vec2)
    private var column9 by ArrayItemDelegate(8, ::Vec2)
    private var column10 by ArrayItemDelegate(9, ::Vec2)
    private var column11 by ArrayItemDelegate(10, ::Vec2)
    private var column12 by ArrayItemDelegate(11, ::Vec2)
    private var column13 by ArrayItemDelegate(12, ::Vec2)
    private var column14 by ArrayItemDelegate(13, ::Vec2)
    private var column15 by ArrayItemDelegate(14, ::Vec2)

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun get(i: Int): Vec2 {
        return when (i) {
            0 -> column1
            1 -> column2
            2 -> column3
            3 -> column4
            4 -> column5
            5 -> column6
            6 -> column7
            7 -> column8
            8 -> column9
            9 -> column10
            10 -> column11
            11 -> column12
            12 -> column13
            13 -> column14
            14 -> column15
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }

    operator fun set(i: Int, value: Vec2) {
        when (i) {
            0 -> column1 = value
            1 -> column2 = value
            2 -> column3 = value
            3 -> column4 = value
            4 -> column5 = value
            5 -> column6 = value
            6 -> column7 = value
            7 -> column8 = value
            8 -> column9 = value
            9 -> column10 = value
            10 -> column11 = value
            11 -> column12 = value
            12 -> column13 = value
            13 -> column14 = value
            14 -> column15 = value
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }
}

class Vec3Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec3"
    override var value: String? = null

    private var column1 by ArrayItemDelegate(0, ::Vec3)
    private var column2 by ArrayItemDelegate(1, ::Vec3)
    private var column3 by ArrayItemDelegate(2, ::Vec3)
    private var column4 by ArrayItemDelegate(3, ::Vec3)
    private var column5 by ArrayItemDelegate(4, ::Vec3)
    private var column6 by ArrayItemDelegate(5, ::Vec3)
    private var column7 by ArrayItemDelegate(6, ::Vec3)
    private var column8 by ArrayItemDelegate(7, ::Vec3)
    private var column9 by ArrayItemDelegate(8, ::Vec3)
    private var column10 by ArrayItemDelegate(9, ::Vec3)
    private var column11 by ArrayItemDelegate(10, ::Vec3)
    private var column12 by ArrayItemDelegate(11, ::Vec3)
    private var column13 by ArrayItemDelegate(12, ::Vec3)
    private var column14 by ArrayItemDelegate(13, ::Vec3)
    private var column15 by ArrayItemDelegate(14, ::Vec3)

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun get(i: Int): Vec3 {
        return when (i) {
            0 -> column1
            1 -> column2
            2 -> column3
            3 -> column4
            4 -> column5
            5 -> column6
            6 -> column7
            7 -> column8
            8 -> column9
            9 -> column10
            10 -> column11
            11 -> column12
            12 -> column13
            13 -> column14
            14 -> column15
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }

    operator fun set(i: Int, value: Vec3) {
        when (i) {
            0 -> column1 = value
            1 -> column2 = value
            2 -> column3 = value
            3 -> column4 = value
            4 -> column5 = value
            5 -> column6 = value
            6 -> column7 = value
            7 -> column8 = value
            8 -> column9 = value
            9 -> column10 = value
            10 -> column11 = value
            11 -> column12 = value
            12 -> column13 = value
            13 -> column14 = value
            14 -> column15 = value
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }
}

class Vec4Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec4"
    override var value: String? = null

    private var column1 by ArrayItemDelegate(0, ::Vec4)
    private var column2 by ArrayItemDelegate(1, ::Vec4)
    private var column3 by ArrayItemDelegate(2, ::Vec4)
    private var column4 by ArrayItemDelegate(3, ::Vec4)
    private var column5 by ArrayItemDelegate(4, ::Vec4)
    private var column6 by ArrayItemDelegate(5, ::Vec4)
    private var column7 by ArrayItemDelegate(6, ::Vec4)
    private var column8 by ArrayItemDelegate(7, ::Vec4)
    private var column9 by ArrayItemDelegate(8, ::Vec4)
    private var column10 by ArrayItemDelegate(9, ::Vec4)
    private var column11 by ArrayItemDelegate(10, ::Vec4)
    private var column12 by ArrayItemDelegate(11, ::Vec4)
    private var column13 by ArrayItemDelegate(12, ::Vec4)
    private var column14 by ArrayItemDelegate(13, ::Vec4)
    private var column15 by ArrayItemDelegate(14, ::Vec4)

    constructor(builder: GlslGenerator, value: String) : this(builder) {
        this.value = value
    }

    operator fun get(i: Int): Vec4 {
        return when (i) {
            0 -> column1
            1 -> column2
            2 -> column3
            3 -> column4
            4 -> column5
            5 -> column6
            6 -> column7
            7 -> column8
            8 -> column9
            9 -> column10
            10 -> column11
            11 -> column12
            12 -> column13
            13 -> column14
            14 -> column15
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }

    operator fun set(i: Int, value: Vec4) {
        when (i) {
            0 -> column1 = value
            1 -> column2 = value
            2 -> column3 = value
            3 -> column4 = value
            4 -> column5 = value
            5 -> column6 = value
            6 -> column7 = value
            7 -> column8 = value
            8 -> column9 = value
            9 -> column10 = value
            10 -> column11 = value
            11 -> column12 = value
            12 -> column13 = value
            13 -> column14 = value
            14 -> column15 = value
            else -> throw Error("Array index $i out of range [0..14]")
        }
    }
}