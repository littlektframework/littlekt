package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
abstract class SubFragmentShaderBuilder : SubShaderBuilder() {
    abstract fun material(group: Int)

    abstract fun main(entryPoint: String = "fs_main")
}
