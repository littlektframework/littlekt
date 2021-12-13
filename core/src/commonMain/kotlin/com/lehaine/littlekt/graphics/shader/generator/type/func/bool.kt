package com.lehaine.littlekt.graphics.shader.generator.type.func

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Bool
import com.lehaine.littlekt.graphics.shader.generator.type.Func
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/13/2021
 */
class BoolFunc(override val builder: GlslGenerator) : Func<Bool> {
    override val typeName: String = "bool"
    override var value: String? = null
    override val type: KClass<Bool> = Bool::class
}