package com.lehaine.littlekt.graph.node.render

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.util.DepthStencilMode
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
                    ?: error("Unable to set u_projection. u_projection uniform was either not created or set correctly.")
            }
        }

    var texture: Texture? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_texture." }
                value.bind()
                uTexture?.apply(shader)
                    ?: error("Unable to set u_texture. u_texture uniform was either not created or set correctly.")
            }
        }
    var model: Mat4? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_model." }
                uModel?.apply(shader, value)
                    ?: error("Unable to set u_model. u_model uniform was either not created or set correctly.")
            }
        }

    var modelInv: Mat4? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_modelInv." }
                uModelInv?.apply(shader, value)
                    ?: error("Unable to set u_modelInv. u_modelInv uniform was either not created or set correctly.")
            }
        }

    var joints: Array<Mat4>? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_joints." }
                uJoints?.apply(shader, value)
                    ?: error("Unable to set u_joints. u_joints uniform was either not created or set correctly.")
            }
        }

    var useJoints: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_useJoints." }
            uUseJoints?.apply(shader, value)
                ?: error("Unable to set u_useJoints. u_useJoints uniform was either not created or set correctly.")

        }

    var lightColor: Color = Color.WHITE
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_lightColor." }
            uLightColor?.apply(shader, value)
                ?: error("Unable to set u_lightColor. u_lightColor uniform was either not created or set correctly.")
        }

    var ambientStrength = 0.1f
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_ambientStrength." }
            uAmbientStrength?.apply(shader, value)
                ?: error("Unable to set u_ambientStrength. u_ambientStrength uniform was either not created or set correctly.")
        }

    var specularStrength = 0.5f
        set(value) {
            field = value
            val shader = checkNotNull(shader) { "Shader is null! Unable to set u_specularStrength." }
            uSpecularStrength?.apply(shader, value)
                ?: error("Unable to set u_specularStrength. u_specularStrength uniform was either not created or set correctly.")
        }

    var lightPosition: Vec3f? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_lightPosition." }
                uLightPosition?.apply(shader, value)
                    ?: error("Unable to set u_lightPosition. u_lightPosition uniform was either not created or set correctly.")
            }
        }

    var viewPosition: Vec3f? = null
        set(value) {
            field = value
            if (value != null) {
                val shader = checkNotNull(shader) { "Shader is null! Unable to set u_viewPosition." }
                uViewPosition?.apply(shader, value)
                    ?: error("Unable to set u_viewPosition. u_viewPosition uniform was either not created or set correctly.")
            }
        }

    private var uProjection: ShaderParameter.UniformMat4? = null
    private var uTexture: ShaderParameter.UniformSample2D? = null
    private var uModel: ShaderParameter.UniformMat4? = null
    private var uModelInv: ShaderParameter.UniformMat4? = null

    private var uJoints: ShaderParameter.UniformArrayMat4? = null
    private var uUseJoints: ShaderParameter.UniformBoolean? = null

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
                U_MODEL_INV -> uModelInv = it as ShaderParameter.UniformMat4
                U_JOINTS -> uJoints = it as ShaderParameter.UniformArrayMat4
                U_USE_JOINTS -> uUseJoints = it as ShaderParameter.UniformBoolean
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
        val U_MODEL_INV = "u_modelInv"
        val U_LIGHT_COLOR = "u_lightColor"
        val U_LIGHT_POSITION = "u_lightPosition"
        val U_AMBIENT_STRENGTH = "u_ambientStrength"
        val U_SPECULAR_STRENGTH = "u_specularStrength"
        val U_VIEW_POSITION = "u_viewPosition"
        val U_JOINTS = "u_joints"
        val U_USE_JOINTS = "u_useJoints"
    }

}