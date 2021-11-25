package com.lehaine.littlekt.graphics.shader.generator.type

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator

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