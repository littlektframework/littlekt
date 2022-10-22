package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class TextVertexShader : VertexShaderModel() {
    val uProjTrans get() = parameters[0] as ShaderParameter.UniformMat4
    private val u_projTrans by uniform(::Mat4)
    private val a_position by attribute(::Vec4)
    private val a_texCoord0 by attribute(::Vec2)
    private val a_color by attribute(::Vec4)
    private var v_color by varying(::Vec4)
    private var v_texCoords by varying(::Vec2)

    init {
        v_texCoords = a_texCoord0
        v_color = a_color
        gl_Position = u_projTrans * a_position
    }
}

/**
 * @author Colton Daily
 * @date 11/29/2021
 */
class TextFragmentShader : FragmentShaderModel() {
    val uColor = ShaderParameter.UniformVec4("u_color")
    val uTexture = ShaderParameter.UniformSample2D("u_texture")

    override val parameters: MutableList<ShaderParameter> = mutableListOf(uColor, uTexture)

    // language=GLSL
    override var source: String = """
        uniform sampler2D u_texture;
        uniform vec4 u_color;

        varying vec4 v_color;
        varying vec2 v_texCoords;

        void main()  {
            // Get samples for -2/3 and -1/3
            vec2 valueL = texture2D(u_texture, vec2(v_texCoords.x + dFdx(v_texCoords.x), v_texCoords.y)).yz * 255.0;
            vec2 lowerL = mod(valueL, 16.0);
            vec2 upperL = (valueL - lowerL) / 16.0;
            vec2 alphaL = min(abs(upperL - lowerL), 2.0);
    
            // Get samples for 0, +1/3, and +2/3
            vec3 valueR = texture2D(u_texture, v_texCoords).xyz * 255.0;
            vec3 lowerR = mod(valueR, 16.0);
            vec3 upperR = (valueR - lowerR) / 16.0;
            vec3 alphaR = min(abs(upperR - lowerR), 2.0);
    
            // Average the energy over the pixels on either side
            vec4 rgba = vec4(
                (alphaR.x + alphaR.y + alphaR.z) / 6.0,
                (alphaL.y + alphaR.x + alphaR.y) / 6.0,
                (alphaL.x + alphaL.y + alphaR.x) / 6.0,
                0.0);
    
            // Optionally scale by a color
            gl_FragColor = u_color.a == 0.0 ? 1.0 - rgba : u_color * rgba;
        }
    """.trimIndent()

    //    val uColor get() = parameters[0] as ShaderParameter.UniformVec4
//    val uTex get() = parameters[1] as ShaderParameter.UniformSample2D

//    private val u_color by uniform(::Vec4)
//    private val u_texture by uniform(::Sampler2D)
//    private val v_color by varying(::Vec4)
//    private val v_texCoords by varying(::Vec2)

//    init {
//        // Get samples for -2/3 and -1/3
//        val valueL by vec2(texture2D(u_texture, vec2Lit(v_texCoords.x + dFdx(v_texCoords.x), v_texCoords.y)).yz * 255f)
//        val lowerL by vec2(mod(valueL, 16f))
//        val upperL by vec2((valueL - lowerL) / 16f)
//        val alphaL by vec2(min(abs(upperL - lowerL), 2f))
//
//        // Get samples for 0, +1/3, and +2/3
//        val valueR by vec3(texture2D(u_texture, v_texCoords).xyz * 255f)
//        val lowerR by vec3(mod(valueR, 16f))
//        val upperR by vec3((valueR - lowerR) / 16f)
//        val alphaR by vec3(min(abs(upperR - lowerR), 2f))
//
//        // Average the energy over the pixels on either side
//        val rgba by vec4(
//            x = (alphaR.x + alphaR.y + alphaR.z) / 6f,
//            y = (alphaL.y + alphaR.x + alphaR.y) / 6f,
//            z = (alphaL.x + alphaL.y + alphaR.x) / 6f,
//            w = 0f
//        )
//        gl_FragColor = ternary(
//            u_color.a eq 0f,
//            left = 1f - rgba,
//            right = u_color * rgba
//        )
//    }
}