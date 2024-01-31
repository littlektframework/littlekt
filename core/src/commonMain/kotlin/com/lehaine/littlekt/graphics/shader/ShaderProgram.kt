package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.createIntBuffer
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.Preparable
import com.lehaine.littlekt.graphics.gl.*
import kotlin.math.min

class ShaderProgram<V : VertexShader, F : FragmentShader>(
    val vertexShader: V,
    val fragmentShader: F,
) : Preparable, Disposable {
    companion object {
        const val POSITION_ATTRIBUTE = "a_position"

        const val NORMAL_ATTRIBUTE = "a_normal"

        const val COLOR_ATTRIBUTE = "a_color"

        const val TEXCOORD_ATTRIBUTE = "a_texCoord"

        const val TANGENT_ATTRIBUTE = "a_tangent"

        const val BINORMAL_ATTRIBUTE = "a_binormal"

        const val WEIGHT_ATTRIBUTE = "a_weight"

        const val JOINT_ATTRIBUTE = "a_joint"

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

    /**
     * Generates the source for each shader and compiles them. A shader program is created, the shaders attached,
     * and the program linked. Lastly, the uniforms and attributes are fetched.
     */
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
        }

        fragmentShader.parameters.forEach {
            if (it.name == U_TEXTURE) {
                uTexture = it as ShaderParameter.UniformSample2D
            }
        }

        fetchUniforms()
        fetchAttributes()
    }

    private val params = createIntBuffer(1)
    private val type = createIntBuffer(1)

    private fun fetchUniforms() {
        val gl = gl
        val programGl = programGl
        check(isPrepared && programGl != null && gl != null) { "ShaderProgram is not prepared! Make sure to call prepare(context)." }
        params.clear()
        gl.getProgramiv(programGl, GetProgram.ACTIVE_UNIFORMS, params)
        val numUniforms = params[0]
        uniforms.clear()
        for (i in 0 until numUniforms) {
            params.clear()
            params[0] = 1
            type.clear()
            val name = gl.getActiveUniform(programGl, i, params, type)
            val location = gl.getUniformLocation(programGl, name)
            uniforms[name] = location
        }
    }

    private fun fetchAttributes() {
        val gl = gl
        val programGl = programGl
        check(isPrepared && programGl != null && gl != null) { "ShaderProgram is not prepared! Make sure to call prepare(context)." }
        params.clear()
        gl.getProgramiv(programGl, GetProgram.ACTIVE_ATTRIBUTES, params)
        val numAttributes = params[0]
        attributes.clear()
        for (i in 0 until numAttributes) {
            params.clear()
            params[0] = 1
            type.clear()
            val name = gl.getActiveAttrib(programGl, i, params, type)
            val location = gl.getAttribLocation(programGl, name)
            attributes[name] = location
        }
    }

    /**
     * @param name the name of the attribute
     * @return the attribute location if it exists; `-1` otherwise
     */
    fun getAttrib(name: String): Int =
        attributes[name] ?: -1

    /**
     * @param name the name of the uniform
     * @return the [UniformLocation] if it exists; `null` otherwise
     */
    fun getUniform(name: String): UniformLocation? {
        return uniforms[name]
    }

    /**
     * Binds the shader program after it has been prepared.
     */
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderProgram<*, *>

        if (vertexShader != other.vertexShader) return false
        if (fragmentShader != other.fragmentShader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertexShader.hashCode()
        result = 31 * result + fragmentShader.hashCode()
        return result
    }
}