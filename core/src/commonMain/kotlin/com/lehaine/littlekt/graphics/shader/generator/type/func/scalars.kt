package com.lehaine.littlekt.graphics.shader.generator.type.func

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class FloatFunc(override val builder: GlslGenerator) : Func<GLFloat> {
    override val typeName: String = "float"
    override var value: String? = null
    override val type: KClass<GLFloat> = GLFloat::class
}

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class IntFunc(override val builder: GlslGenerator) : Func<GLInt> {
    override val typeName: String = "int"
    override var value: String? = null
    override val type: KClass<GLInt> = GLInt::class
}