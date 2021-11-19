package com.lehaine.littlekt.shader

import com.lehaine.littlekt.GL
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
object ShaderUtils {

    fun createShaderProgram(gl: GL, vertexShader: String, fragmentShader: String): ShaderProgramReference {
        val vertex = compileShader(gl, vertexShader, GL.VERTEX_SHADER)
        val fragment = compileShader(gl, fragmentShader, GL.FRAGMENT_SHADER)

        val shaderProgram = gl.createProgram()
        gl.attachShader(shaderProgram, vertex)
        gl.attachShader(shaderProgram, fragment)
        gl.linkProgram(shaderProgram)

        if (!gl.getProgramParameterB(shaderProgram, GL.LINK_STATUS)) {
            val log = gl.getProgramInfoLog(shaderProgram)
            throw RuntimeException("Shader compilation error: $log")
        }
        return shaderProgram
    }

    fun compileShader(gl: GL, shaderSrc: String, type: Int): ShaderReference {
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