package com.lehaine.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
interface ShaderSourceProvider {
    fun getSource(): String
}

interface ShaderProgramSources {
    val vertex: String
    val fragment: String
}