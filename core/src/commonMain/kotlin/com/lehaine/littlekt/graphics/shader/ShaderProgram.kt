package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.gl.*
import kotlin.math.min

class ShaderProgram(
    val gl: GL,
    vertexShader: VertexShader,
    fragmentShader: FragmentShader,
) : Disposable {
    companion object {
        /** default name for position attributes  */
        const val POSITION_ATTRIBUTE = "a_position"

        /** default name for normal attributes  */
        const val NORMAL_ATTRIBUTE = "a_normal"

        /** default name for color attributes  */
        const val COLOR_ATTRIBUTE = "a_color"

        /** default name for texcoords attributes, append texture unit number  */
        const val TEXCOORD_ATTRIBUTE = "a_texCoord"

        const val U_PROJ_TRANS_UNIFORM = "u_projTrans"
        const val U_TEXTURE = "u_texture"
    }

    private val vertexShaderReference: GlShader
    private val fragmentShaderReference: GlShader
    private val programGl: GlShaderProgram

    private val attributes = mutableMapOf<String, Int>()
    private val uniforms = mutableMapOf<String, UniformLocation>()

    var uProjTrans: ShaderParameter.UniformMat4? = null
        private set
    var uTexture: ShaderParameter.UniformSample2D? = null

    init {
        vertexShaderReference = compileShader(ShaderType.VERTEX_SHADER, vertexShader.source)
        fragmentShaderReference = compileShader(ShaderType.FRAGMENT_SHADER, fragmentShader.source)

        programGl = gl.createProgram()
        gl.attachShader(programGl, vertexShaderReference)
        gl.attachShader(programGl, fragmentShaderReference)
        gl.linkProgram(programGl)

        if (!gl.getProgramParameterB(programGl, GetProgram.LINK_STATUS)) {
            val log = gl.getProgramInfoLog(programGl)
            throw RuntimeException("Shader compilation error: $log")
        }

        vertexShader.parameters.forEach {
            if (it.name == U_PROJ_TRANS_UNIFORM) {
                uProjTrans = it as ShaderParameter.UniformMat4
            }
            it.create(this)
        }

        fragmentShader.parameters.forEach {
            if (it.name == U_TEXTURE) {
                uTexture = it as ShaderParameter.UniformSample2D
            }
            it.create(this)
        }
    }

    fun createAttrib(name: String) {
        attributes[name] = gl.getAttribLocation(programGl, name)
    }

    fun createUniform(name: String) {
        uniforms[name] = gl.getUniformLocation(programGl, name)
    }

    fun getAttrib(name: String): Int =
        attributes[name] ?: throw IllegalStateException("Attributes '$name' not created!")

    fun getUniform(name: String): UniformLocation {
        return uniforms[name] ?: throw IllegalStateException("Uniform '$name' not created!")
    }

    fun bind() {
        gl.useProgram(programGl)
    }

    private fun compileShader(type: ShaderType, shaderSrc: String): GlShader {
        val shader = gl.createShader(type)
        gl.shaderSource(shader, shaderSrc)
        gl.compileShader(shader)

        if (!gl.getShaderParameterB(shader, GetShader.COMPILE_STATUS)) {
            val log = gl.getShaderInfoLog(shader)
            gl.deleteShader(shader)
            throw RuntimeException(
                "Shader compilation error: $log (${
                    shaderSrc.substring(
                        0,
                        min(shaderSrc.length, 128)
                    )
                })"
            )
        }
        return shader
    }

    override fun dispose() {
        gl.useDefaultProgram()
        gl.deleteShader(vertexShaderReference)
        gl.deleteShader(fragmentShaderReference)
        gl.deleteProgram(programGl)
    }
}