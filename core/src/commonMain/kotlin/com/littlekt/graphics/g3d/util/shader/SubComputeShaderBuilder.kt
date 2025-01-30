package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
abstract class SubComputeShaderBuilder : SubShaderBuilder() {

    abstract fun main(entryPoint: String = "cmp_main")
}
