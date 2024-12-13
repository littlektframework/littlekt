package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
abstract class SubFragmentShaderBuilder {
    protected val parts = mutableListOf<String>()

    abstract fun material(group: Int)

    abstract fun main(entryPoint: String = "fs_main")

    fun build(): String {
        return parts.joinToString("\n")
    }
}
