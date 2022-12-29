package com.lehaine.littlekt.graph.node.render

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec3f

/**
 * @author Colton Daily
 * @date 12/27/2022
 */
open class ModelMaterial(
    /**
     * The [ShaderProgram] that this material will use for rendering
     */
    shader: ShaderProgram<*, *>,
) : Material(shader) {

    init {
        depthStencilMode = DepthStencilMode.Default
    }

    var projection: Mat4? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_projection." }
                uProjection?.apply(shader, value)
            }
        }

    var texture: Texture? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_texture." }
                value.bind()
                uTexture?.apply(shader)
            }
        }
    var model: Mat4? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_model." }
                uModel?.apply(shader, value)
            }
        }

    var lightColor: Color = Color.WHITE
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_lightColor." }
            uLightColor?.apply(shader, value)
        }

    var ambientStrength = 0.1f
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_ambientStrength." }
            uAmbientStrength?.apply(shader, value)
        }

    var specularStrength = 0.5f
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_specularStrength." }
            uAmbientStrength?.apply(shader, value)
        }

    var lightPosition: Vec3f? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_lightPosition." }
                uLightPosition?.apply(shader, value)
            }
        }

    var viewPosition: Vec3f? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_viewPosition." }
                uViewPosition?.apply(shader, value)
            }
        }

    private var uProjection: ShaderParameter.UniformMat4? = null
    private var uTexture: ShaderParameter.UniformSample2D? = null
    private var uModel: ShaderParameter.UniformMat4? = null

    private var uLightColor: ShaderParameter.UniformVec4? = null
    private var uAmbientStrength: ShaderParameter.UniformFloat? = null
    private var uSpecularStrength: ShaderParameter.UniformFloat? = null
    private var uLightPosition: ShaderParameter.UniformVec3? = null
    private var uViewPosition: ShaderParameter.UniformVec3? = null

    init {
        require(shader.prepared) { "Shader must be prepared before creating a ModelMaterial!" }

        shader.vertexShader.parameters.forEach {
            when (it.name) {
                U_PROJ_TRANS -> uProjection = it as ShaderParameter.UniformMat4
                U_MODEL -> uModel = it as ShaderParameter.UniformMat4
            }
        }

        shader.fragmentShader.parameters.forEach {
            when (it.name) {
                U_TEXTURE -> uTexture = it as ShaderParameter.UniformSample2D
                U_LIGHT_COLOR -> uLightColor = it as ShaderParameter.UniformVec4
                U_AMBIENT_STRENGTH -> uAmbientStrength = it as ShaderParameter.UniformFloat
                U_SPECULAR_STRENGTH -> uSpecularStrength = it as ShaderParameter.UniformFloat
                U_LIGHT_POSITION -> uLightPosition = it as ShaderParameter.UniformVec3
                U_VIEW_POSITION -> uViewPosition = it as ShaderParameter.UniformVec3
            }
        }
    }

    companion object {
        val U_PROJ_TRANS = "u_projection"
        val U_TEXTURE = ShaderProgram.U_TEXTURE
        val U_MODEL = "u_model"
        val U_LIGHT_COLOR = "u_lightColor"
        val U_LIGHT_POSITION = "u_lightPosition"
        val U_AMBIENT_STRENGTH = "u_ambientStrength"
        val U_SPECULAR_STRENGTH = "u_specularStrength"
        val U_VIEW_POSITION = "u_viewPosition"
    }

}