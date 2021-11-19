package com.lehaine.littlekt.shader

import com.lehaine.littlekt.GL
import com.lehaine.littlekt.shader.fragment.FragmentShader
import com.lehaine.littlekt.shader.vertex.VertexShader
import kotlin.math.min

class ShaderProgram(
    val gl: GL,
    val vertexShader: VertexShader,
    val fragmentShader: FragmentShader,
) : GL by gl {
    private val vertexShaderReference: ShaderReference
    private val fragmentShaderReference: ShaderReference
    private val programReference: ShaderProgramReference

    private val attributes = mutableMapOf<String, Int>()
    private val uniforms = mutableMapOf<String, Uniform>()

    init {
        vertexShaderReference = compileShader(GL.VERTEX_SHADER, vertexShader.toString())
        fragmentShaderReference = compileShader(GL.FRAGMENT_SHADER, fragmentShader.toString())

        programReference = gl.createProgram()
        gl.attachShader(programReference, vertexShaderReference)
        gl.attachShader(programReference, fragmentShaderReference)
        gl.linkProgram(programReference)

        if (!gl.getProgramParameterB(programReference, GL.LINK_STATUS)) {
            val log = gl.getProgramInfoLog(programReference)
            throw RuntimeException("Shader compilation error: $log")
        }

        vertexShader.parameters.forEach {
            it.create(this)
        }

        fragmentShader.parameters.forEach {
            it.create(this)
        }
    }

    fun createAttrib(name: String) {
        attributes[name] = gl.getAttribLocation(programReference, name)
    }

    fun createUniform(name: String) {
        uniforms[name] = gl.getUniformLocation(programReference, name)
    }

    fun getAttrib(name: String): Int =
        attributes[name] ?: throw IllegalStateException("Attributes '$name' not created!")

    fun getUniform(name: String): Uniform {
        return uniforms[name] ?: throw IllegalStateException("Uniform '$name' not created!")
    }

    private fun compileShader(type: Int, shaderSrc: String): ShaderReference {
        val shader = gl.createShader(type)
        gl.shaderSource(shader, shaderSrc)
        gl.compileShader(shader)

        if (!gl.getShaderParameterB(shader, GL.COMPILE_STATUS)) {
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
}