package com.lehaine.littlekt.graphics.shader.generator.type.vec

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Variable
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class Vec2Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec2"
    override var value: String? = null

    operator fun get(i: GLInt): Vec2 {
        val result = Vec2(builder)
        result.value = "$value[${i.value}]"
        return result
    }
}

class Vec3Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec3"
    override var value: String? = null

    operator fun get(i: GLInt): Vec3 {
        val result = Vec3(builder)
        result.value = "$value[${i.value}]"
        return result
    }
}

class Vec4Array(override val builder: GlslGenerator) : Variable {
    override val typeName: String = "vec4"
    override var value: String? = null

    operator fun get(i: GLInt): Vec4 {
        val result = Vec4(builder)
        result.value = "$value[${i.value}]"
        return result
    }
}