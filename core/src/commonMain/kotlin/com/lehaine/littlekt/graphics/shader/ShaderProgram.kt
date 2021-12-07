package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.Preparable
import com.lehaine.littlekt.graphics.gl.*
import kotlin.math.min

class ShaderProgram<V : VertexShader, F : FragmentShader>(
    val vertexShader: V,
    val fragmentShader: F,
) : Preparable, Disposable {
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

    var gl: GL? = null
        private set
    var uProjTrans: ShaderParameter.UniformMat4? = null
        private set
    var uTexture: ShaderParameter.UniformSample2D? = null

    override val prepared: Boolean
        get() = isPrepared

    private var vertexShaderReference: GlShader? = null
    private var fragmentShaderReference: GlShader? = null
    private var programGl: GlShaderProgram? = null

    private val attributes = mutableMapOf<String, Int>()
    private val uniforms = mutableMapOf<String, UniformLocation>()

    private var isPrepared = false


    override fun prepare(context: Context) {
        val gl = context.gl.also { gl = it }
        if (vertexShader is VertexShaderModel) {
            vertexShader.generate(context)
        }
        if (fragmentShader is FragmentShaderModel) {
            fragmentShader.generate(context)
        }
        val vertexShaderReference =
            compileShader(ShaderType.VERTEX_SHADER, vertexShader.source).also { vertexShaderReference = it }
        val fragmentShaderReference =
            compileShader(ShaderType.FRAGMENT_SHADER, fragmentShader.source).also { fragmentShaderReference = it }

        val programGl = gl.createProgram().also { programGl = it }
        gl.attachShader(programGl, vertexShaderReference)
        gl.attachShader(programGl, fragmentShaderReference)
        gl.linkProgram(programGl)

        if (!gl.getProgramParameterB(programGl, GetProgram.LINK_STATUS)) {
            val log = gl.getProgramInfoLog(programGl)
            throw RuntimeException("Shader compilation error: $log")
        }

        isPrepared = true
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
        val gl = gl
        val programGl = programGl
        check(isPrepared && programGl != null && gl != null) { "ShaderProgram is not prepared! Make sure to call prepare(context)." }
        attributes[name] = gl.getAttribLocation(programGl, name)
    }

    fun createUniform(name: String) {
        val gl = gl
        val programGl = programGl
        check(isPrepared && programGl != null && gl != null) { "ShaderProgram is not prepared! Make sure to call prepare(context)." }
        uniforms[name] = gl.getUniformLocation(programGl, name)
    }

    fun getAttrib(name: String): Int =
        attributes[name] ?: throw IllegalStateException("Attributes '$name' not created!")

    fun getUniform(name: String): UniformLocation {
        return uniforms[name] ?: throw IllegalStateException("Uniform '$name' not created!")
    }

    fun bind() {
        val gl = gl
        val programGl = programGl
        check(isPrepared && programGl != null && gl != null) { "ShaderProgram is not prepared! Make sure to call prepare(context)." }
        gl.useProgram(programGl)
    }

    private fun compileShader(type: ShaderType, shaderSrc: String): GlShader {
        val gl = gl
        check(gl != null) { "Unable to compile shaders due to gl not being setting!" }
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
        val gl = gl
        if (!prepared || gl == null) return
        gl.useDefaultProgram()
        vertexShaderReference?.let { gl.deleteShader(it) }
        fragmentShaderReference?.let { gl.deleteShader(it) }
        programGl?.let { gl.deleteProgram(it) }

    }
}