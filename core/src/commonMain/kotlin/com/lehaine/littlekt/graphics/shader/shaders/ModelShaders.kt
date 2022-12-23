package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.graphics.shader.generator.type.mat.Mat4
import com.lehaine.littlekt.graphics.shader.generator.type.scalar.GLFloat
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec3
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4

/**
 * @author Colton Daily
 * @date 12/20/2022
 */
class ModelVertexShader : VertexShaderModel() {
    val uProjTrans get() = parameters[0] as ShaderParameter.UniformMat4
    val uModel get() = parameters[1] as ShaderParameter.UniformMat4

    private val u_projTrans by uniform(::Mat4)
    private val u_model by uniform(::Mat4)

    private val a_position by attribute(::Vec3)
    private val a_color by attribute(::Vec4)
    private val a_normal by attribute(::Vec3)

    //  private val a_texCoord0 by attribute(::Vec2)
    private var v_color by varying(::Vec4)
    private var v_normal by varying(::Vec3)
    private var v_fragPosition by varying(::Vec3)
    //   private var v_texCoords by varying(::Vec2)

    init {
        //      v_texCoords = a_texCoord0
        v_color = a_color
        v_normal = mat3(transpose(inverse(u_model))) * a_normal
        v_fragPosition = vec3(u_model * vec4(a_position, 1f).lit).lit
        gl_Position = u_projTrans * vec4(v_fragPosition, 1f).lit
    }
}

class ModelFragmentShader : FragmentShaderModel() {
    //  val uTexture get() = parameters[0] as ShaderParameter.UniformSample2D
    val uLightColor get() = parameters[0] as ShaderParameter.UniformVec4
    val uAmbientStrength get() = parameters[1] as ShaderParameter.UniformFloat
    val uLightPosition get() = parameters[2] as ShaderParameter.UniformVec3

    //   private val u_texture by uniform(::Sampler2D)
    private val u_lightColor by uniform(::Vec4)
    private val u_ambientStrength by uniform(::GLFloat)
    private val u_lightPosition by uniform(::Vec3)

    private val v_color by varying(::Vec4)
    private val v_normal by varying(::Vec3)
    private val v_fragPos by varying(::Vec3)
    //   private val v_texCoords by varying(::Vec2)

    init {
        // diffuse light
        var norm by vec3()
        norm = normalize(v_normal)
        var lightDir by vec3()
        lightDir = normalize(u_lightPosition - v_fragPos)
        var diff by float()
        diff = max(dot(norm, lightDir), 0f)
        var diffuse by vec3()
        diffuse = diff * u_lightColor.xyz

        // ambient light
        var ambient by vec3()
        ambient = u_ambientStrength * u_lightColor.xyz
        var result by vec3()
        result = (ambient + diffuse) * vec3(1f, 0.5f, 0.31f).lit

        gl_FragColor = vec4(result, 1f).lit
    }
}