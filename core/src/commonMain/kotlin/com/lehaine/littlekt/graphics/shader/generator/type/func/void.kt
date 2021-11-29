package com.lehaine.littlekt.graphics.shader.generator.type.func

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import com.lehaine.littlekt.graphics.shader.generator.type.Func

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class Void(override val builder: GlslGenerator) : Func {
    override val typeName: String = "void"
    override var value: String? = null


}