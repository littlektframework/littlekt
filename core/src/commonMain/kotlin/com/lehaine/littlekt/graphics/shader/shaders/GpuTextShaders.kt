package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.func.BoolFunc
import com.lehaine.littlekt.graphics.shader.generator.type.func.FloatFunc
import com.lehaine.littlekt.graphics.shader.generator.type.func.Vec2Func
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class GpuTextVertexShader : VertexShaderModel() {
    val uAtlasSampler get() = parameters[0] as ShaderParameter.UniformSample2D
    val uProjTrans get() = parameters[1] as ShaderParameter.UniformMat4

    val u_texture by uniform(::Sampler2D)
    val u_projTrans by uniform(::Mat4)

    val a_position by attribute(::Vec2)
    val a_color by attribute(::Vec4)
    val a_texCoord0 by attribute(::Vec2)

    var v_color by varying(::Vec4)
    var v_rect by varying(::Vec4)
    var v_coord by varying(::Vec2)

    val uShortFromVec2 by Func(::FloatFunc, ::Vec2) {
        65280f * it.y + 255f * it.x
    }

    val vec2FromPixel by Func(::Vec2Func, ::Vec2) {
        val result by vec2()
        val tex = texture2D(u_texture, it + 0.5f)
        result.x = uShortFromVec2(tex.xy).lit
        result.y = uShortFromVec2(tex.zw).lit
        result
    }

    init {
        v_color = a_color
        v_coord = a_texCoord0
        v_rect = vec4Lit(vec2FromPixel(a_texCoord0).lit, vec2FromPixel(vec2Lit(a_texCoord0.x + 1f, a_texCoord0.y)).lit)
        gl_Position = u_projTrans * vec4Lit(a_position, 0f, 1f)
    }
}

class GpuTextFragmentShader : FragmentShaderModel() {
    val uAtlasSampler get() = parameters[0] as ShaderParameter.UniformSample2D

    val pi = 3.1415926535897932384626433832795f
    val numSS = 4
    val pixelWindowSize = 1f

    val u_texture by uniform(::Sampler2D)

    val v_color by varying(::Vec4)
    val v_rect by varying(::Vec4)
    val v_coord by varying(::Vec2)

    val positionAt by Func(::FloatFunc, ::GLFloat, ::GLFloat, ::GLFloat, ::GLFloat) { p0, p1, p2, t ->
        val mt by float(1f - t)
        mt * mt * p0 + 2f * t * mt * p1 + t * t * p2
    }

    val tangentAt by Func(::FloatFunc, ::GLFloat, ::GLFloat, ::GLFloat, ::GLFloat) { p0, p1, p2, t ->
        2f * (1f - t) * (p1 - p0) + 2f * t * (p2 - p1)
    }

    val almostEqual by Func(::BoolFunc, ::GLFloat, ::GLFloat) { a, b ->
        abs(a - b) lt 1e-5f
    }

    init {
        gl_FragColor = vec4Lit(1f, 1f, 1f, 1f)
    }
}