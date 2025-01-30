package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 1/4/2025
 */
abstract class SubShaderBuilder {
    val parts = mutableListOf<String>()

    fun build(): String {
        return parts.joinToString("\n")
    }
}
