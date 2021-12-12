package com.lehaine.littlekt.graphics.shader.generator.type.func

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Vec2Func(override val builder: GlslGenerator) : Func<Vec2> {
    override val typeName: String = "vec2"
    override var value: String? = null
    override val type: KClass<Vec2> = Vec2::class
}

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Vec3Func(override val builder: GlslGenerator) : Func<Vec3> {
    override val typeName: String = "vec3"
    override var value: String? = null
    override val type: KClass<Vec3> = Vec3::class
}

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Vec4Func(override val builder: GlslGenerator) : Func<Vec4> {
    override val typeName: String = "vec4"
    override var value: String? = null
    override val type: KClass<Vec4> = Vec4::class
}