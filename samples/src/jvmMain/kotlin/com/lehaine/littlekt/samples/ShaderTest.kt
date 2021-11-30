package com.lehaine.littlekt.samples

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.func.FloatFunc
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class FuncShader : FragmentShaderModel() {
    val doWork by Void(::Vec2) {
        val b by float(5f)
        val a by vec2(0f, 1f)
        a.x *= b
        a.y *= b
        a.x = it.y
        a.y = it.x
    }

    val noParamWork by Void() {
        val b by float(3f)
        val a by vec4(1f, 0f, 1f, 2f)
        a.x = b
    }

    val calc by Func(::FloatFunc, ::GLFloat) {
        val result by float(5f)
        result * it
    }

    init {
        calc(gl_FragColor.x).lit
        doWork(gl_FragColor.xy)
        noParamWork()
        val b by calc(gl_FragColor.y)
        val x by float(b)
        val newVec2 by vec2(x, b)
        gl_FragColor.xy = newVec2
        gl_FragColor = vec4Lit(0f, 1f, 0f, 1f)
    }
}

fun main() {
    println(FuncShader().source)
}