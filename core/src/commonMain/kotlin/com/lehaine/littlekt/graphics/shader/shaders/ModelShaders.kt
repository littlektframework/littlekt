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
open class ModelVertexShader : VertexShaderModel() {
    val uProjection get() = parameters[0] as ShaderParameter.UniformMat4
    val uModel get() = parameters[1] as ShaderParameter.UniformMat4

    private val u_projection by uniform(::Mat4)
    private val u_model by uniform(::Mat4)

    private val a_position by attribute(::Vec3)

    // private val a_color by attribute(::Vec4)
    private val a_normal by attribute(::Vec3)

    //  private val a_texCoord0 by attribute(::Vec2)
    //private var v_color by varying(::Vec4)
    private var v_normal by varying(::Vec3)
    private var v_fragPosition by varying(::Vec3)
    //   private var v_texCoords by varying(::Vec2)

    init {
        //      v_texCoords = a_texCoord0
        //  v_color = a_color
        v_normal = mat3(transpose(inverse(u_model))) * a_normal
        v_fragPosition = vec3(u_model * vec4(a_position, 1f).lit).lit
        gl_Position = u_projection * vec4(v_fragPosition, 1f).lit
    }
}

open class ModelFragmentShader : FragmentShaderModel() {
    //  val uTexture get() = parameters[0] as ShaderParameter.UniformSample2D
    val uLightColor get() = parameters[0] as ShaderParameter.UniformVec4
    val uAmbientStrength get() = parameters[1] as ShaderParameter.UniformFloat
    val uSpecularStrength get() = parameters[2] as ShaderParameter.UniformFloat
    val uLightPosition get() = parameters[3] as ShaderParameter.UniformVec3
    val uViewPosition get() = parameters[4] as ShaderParameter.UniformVec3

    //   private val u_texture by uniform(::Sampler2D)
    private val u_lightColor by uniform(::Vec4)
    private val u_ambientStrength by uniform(::GLFloat)
    private val u_specularStrength by uniform(::GLFloat)
    private val u_lightPosition by uniform(::Vec3)
    private val u_viewPosition by uniform(::Vec3)

    //private val v_color by varying(::Vec4)
    private val v_normal by varying(::Vec3)
    private val v_fragPos by varying(::Vec3)
    //   private val v_texCoords by varying(::Vec2)

    init {
        // diffuse light
        val norm by vec3 { normalize(v_normal) }
        val lightDir by vec3 { normalize(u_lightPosition - v_fragPos) }
        val diffFactor by float { max(dot(norm, lightDir), 0f) }
        val diffColor by vec3 { diffFactor * u_lightColor.xyz }

        // ambient light
        val ambientColor by vec3 { u_ambientStrength * u_lightColor.xyz }

        // specular light
        val viewDir by vec3 { normalize(u_viewPosition - v_fragPos) }
        val reflectDir by vec3 { reflect(-lightDir, norm) }
        val specFactor by float { pow(max(dot(viewDir, reflectDir), 0f), 32f) }
        val specColor by vec3 { u_specularStrength * specFactor * u_lightColor.xyz }

        val result by vec3 { (ambientColor + diffColor + specColor) * vec3(1f, 0.5f, 0.31f).lit }

        gl_FragColor = vec4(result, 1f).lit
    }
}