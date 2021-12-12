package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.func.FloatFunc
import com.lehaine.littlekt.graphics.shader.generator.type.func.Vec2Func
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLInt
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class GpuTextVertexShader : VertexShaderModel() {
    val uAtlasSampler by uniform(::Sampler2D)
    val uProjTrans by uniform(::Mat4)

    val aPosition by attribute(::Vec2)
    val aData by attribute(::GLInt)
    val aColor by attribute(::Vec4)

    var vColor by varying(::Vec4)
    var vRect by varying(::Vec4)
    var vCoord by varying(::Vec2)

    val uShortFromVec2 by Func(::FloatFunc, ::Vec2) {
        65280f * it.y + 255f * it.x
    }

    val vec2FromPixel by Func(::Vec2Func, ::Vec2) {
        val result by vec2()
        val tex = texture2D(uAtlasSampler, it + 0.5f)
        val x by uShortFromVec2(tex.xy)
        val y by uShortFromVec2(tex.zw)
        result.x = x
        result.y = y
        result
    }

    init {
        vColor = aColor

        gl_Position = uProjTrans * vec4Lit(aPosition, 0f, 1f)
    }
}