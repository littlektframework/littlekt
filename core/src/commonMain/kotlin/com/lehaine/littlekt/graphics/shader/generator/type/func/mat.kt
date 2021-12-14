package com.lehaine.littlekt.graphics.shader.generator.type.func

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat2
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat3
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Mat2Func(override val builder: GlslGenerator) : Func<Mat2> {
    override val typeName: String = "mat2"
    override var value: String? = null
    override val type: KClass<Mat2> = Mat2::class
}

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Mat3Func(override val builder: GlslGenerator) : Func<Mat3> {
    override val typeName: String = "mat3"
    override var value: String? = null
    override val type: KClass<Mat3> = Mat3::class
}

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class Mat4Func(override val builder: GlslGenerator) : Func<Mat4> {
    override val typeName: String = "mat4"
    override var value: String? = null
    override val type: KClass<Mat4> = Mat4::class
}