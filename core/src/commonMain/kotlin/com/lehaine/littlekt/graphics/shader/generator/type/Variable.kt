package com.lehaine.littlekt.graphics.shader.generator.type

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface Variable {
    val builder: GlslGenerator
    val typeName: String
    var value: String?
}

interface GenType : Variable
interface Vector : GenType
interface Matrix : Variable

interface Func<T : Any> {
    val builder: GlslGenerator
    val typeName: String
    var value: String?
    val type: KClass<T>
}