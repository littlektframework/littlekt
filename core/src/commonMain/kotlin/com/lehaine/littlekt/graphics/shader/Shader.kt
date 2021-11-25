package com.lehaine.littlekt.graphics.shader

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface Shader {
    var source: String
    val parameters: List<ShaderParameter>
}