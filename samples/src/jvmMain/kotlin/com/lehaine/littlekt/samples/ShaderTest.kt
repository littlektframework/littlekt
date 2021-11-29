package com.lehaine.littlekt.samples

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class FuncShader : FragmentShaderModel() {
    val t by Void(::Vec2) {
        val b by float(5f)
        val a by vec2(0f, 1f)
        a.x *= b
        a.y *= b
    }

    init {
        t(gl_FragColor.xy)
        gl_FragColor = vec4Lit(0f, 1f, 0f, 1f)
    }
}

fun main() {
    println(FuncShader().source)
}