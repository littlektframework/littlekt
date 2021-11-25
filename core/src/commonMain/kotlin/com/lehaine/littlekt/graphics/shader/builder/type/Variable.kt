package com.lehaine.littlekt.graphics.shader.builder.type

import com.lehaine.littlekt.graphics.shader.builder.ShaderBuilder

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface Variable {
    val builder: ShaderBuilder
    val typeName: String
    var value: String?
}

interface GenType : Variable
interface Vector : GenType
interface Matrix : Variable